package com.ctn.offerwall.notification.notification;

import com.ctn.offerwall.common.notification.NotificationChannel;
import com.ctn.offerwall.common.notification.NotificationPriority;
import com.ctn.offerwall.notification.domain.NotificationCampaign;
import com.ctn.offerwall.notification.domain.NotificationDelivery;
import com.ctn.offerwall.notification.domain.NotificationDeliveryStatus;
import com.ctn.offerwall.notification.domain.NotificationSendMode;
import com.ctn.offerwall.notification.exception.AuthorizationDeniedException;
import com.ctn.offerwall.notification.exception.NotificationNotFoundException;
import com.ctn.offerwall.notification.exception.UserInputException;
import com.ctn.offerwall.notification.notification.dto.InAppNotificationResponse;
import com.ctn.offerwall.notification.notification.dto.NotificationCampaignResponse;
import com.ctn.offerwall.notification.notification.dto.NotificationRecipientRequest;
import com.ctn.offerwall.notification.notification.dto.NotificationRequest;
import com.ctn.offerwall.notification.repository.NotificationCampaignRepository;
import com.ctn.offerwall.notification.repository.NotificationDeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationCampaignRepository campaignRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final Clock clock = Clock.systemUTC();

    public NotificationService(NotificationCampaignRepository campaignRepository,
                               NotificationDeliveryRepository deliveryRepository) {
        this.campaignRepository = campaignRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public NotificationCampaignResponse createNotification(NotificationRequest request) {
        validateRequest(request);
        if (request.sourceEventId() != null) {
            return campaignRepository.findWithDeliveriesBySourceEventId(request.sourceEventId())
                    .map(NotificationCampaignResponse::from)
                    .orElseGet(() -> createNewNotification(request));
        }

        return createNewNotification(request);
    }

    private NotificationCampaignResponse createNewNotification(NotificationRequest request) {
        NotificationCampaign campaign = new NotificationCampaign(
                request.title().trim(),
                request.body().trim(),
                request.priority(),
                request.sendMode(),
                request.scheduledFor(),
                request.offerId(),
                request.sourceEventId()
        );

        for (NotificationRecipientRequest recipient : uniqueRecipients(request.recipients()).values()) {
            for (NotificationChannel channel : request.channels()) {
                campaign.addDelivery(createDelivery(campaign, recipient, channel, request.priority(), request.sendMode()));
            }
        }

        return NotificationCampaignResponse.from(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public List<NotificationCampaignResponse> listCampaigns() {
        return campaignRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationCampaignResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationCampaignResponse getCampaign(UUID id) {
        return campaignRepository.findWithDeliveriesById(id)
                .map(NotificationCampaignResponse::from)
                .orElseThrow(() -> new NotificationNotFoundException("Notification campaign was not found."));
    }

    @Transactional(readOnly = true)
    public List<InAppNotificationResponse> getInAppNotifications(UUID userId) {
        return deliveryRepository.findByRecipientUserIdAndChannelOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP).stream()
                .filter(delivery -> delivery.getStatus() != NotificationDeliveryStatus.SKIPPED)
                .map(InAppNotificationResponse::from)
                .toList();
    }

    @Transactional
    public InAppNotificationResponse markInAppRead(UUID deliveryId, UUID requesterUserId) {
        NotificationDelivery delivery = deliveryRepository.findWithCampaignById(deliveryId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification delivery was not found."));
        if (!delivery.getRecipientUserId().equals(requesterUserId)) {
            throw new AuthorizationDeniedException("Notification belongs to another user.");
        }
        if (delivery.getChannel() != NotificationChannel.IN_APP) {
            throw new UserInputException("Only in-app notifications can be marked read.");
        }
        delivery.markRead(Instant.now(clock));
        return InAppNotificationResponse.from(delivery);
    }

    private NotificationDelivery createDelivery(NotificationCampaign campaign,
                                                NotificationRecipientRequest recipient,
                                                NotificationChannel channel,
                                                NotificationPriority priority,
                                                NotificationSendMode sendMode) {
        boolean preferenceEnabled = preferenceEnabled(recipient, channel);
        boolean preferenceBypassed = priority == NotificationPriority.HIGH && !preferenceEnabled;

        if (!preferenceEnabled && !preferenceBypassed) {
            return new NotificationDelivery(
                    campaign,
                    recipient.userId(),
                    trimToNull(recipient.email()),
                    channel,
                    NotificationDeliveryStatus.SKIPPED,
                    false,
                    "Recipient disabled this notification channel.",
                    null
            );
        }

        if (channel == NotificationChannel.EMAIL && trimToNull(recipient.email()) == null) {
            return new NotificationDelivery(
                    campaign,
                    recipient.userId(),
                    null,
                    channel,
                    NotificationDeliveryStatus.FAILED,
                    preferenceBypassed,
                    "Recipient email is required for email notification.",
                    null
            );
        }

        NotificationDeliveryStatus status = sendMode == NotificationSendMode.IMMEDIATE
                ? NotificationDeliveryStatus.SENT
                : NotificationDeliveryStatus.QUEUED;
        Instant sentAt = status == NotificationDeliveryStatus.SENT ? Instant.now(clock) : null;

        return new NotificationDelivery(
                campaign,
                recipient.userId(),
                trimToNull(recipient.email()),
                channel,
                status,
                preferenceBypassed,
                null,
                sentAt
        );
    }

    private void validateRequest(NotificationRequest request) {
        if (request.sendMode() == NotificationSendMode.IMMEDIATE && request.scheduledFor() != null) {
            throw new UserInputException("IMMEDIATE notifications cannot define scheduledFor.");
        }
    }

    private Map<UUID, NotificationRecipientRequest> uniqueRecipients(List<NotificationRecipientRequest> recipients) {
        Map<UUID, NotificationRecipientRequest> unique = new LinkedHashMap<>();
        for (NotificationRecipientRequest recipient : recipients) {
            unique.putIfAbsent(recipient.userId(), recipient);
        }
        return unique;
    }

    private boolean preferenceEnabled(NotificationRecipientRequest recipient, NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> recipient.emailEnabled();
            case IN_APP -> recipient.inAppEnabled();
        };
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
