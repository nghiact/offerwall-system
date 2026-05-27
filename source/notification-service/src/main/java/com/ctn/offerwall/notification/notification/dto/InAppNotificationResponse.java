package com.ctn.offerwall.notification.notification.dto;

import com.ctn.offerwall.common.notification.NotificationPriority;
import com.ctn.offerwall.notification.domain.NotificationDelivery;
import com.ctn.offerwall.notification.domain.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record InAppNotificationResponse(
        UUID deliveryId,
        UUID campaignId,
        UUID recipientUserId,
        String title,
        String body,
        NotificationPriority priority,
        UUID offerId,
        UUID sourceEventId,
        NotificationDeliveryStatus status,
        boolean preferenceBypassed,
        Instant sentAt,
        Instant readAt,
        Instant createdAt
) {

    public static InAppNotificationResponse from(NotificationDelivery delivery) {
        return new InAppNotificationResponse(
                delivery.getId(),
                delivery.getCampaign().getId(),
                delivery.getRecipientUserId(),
                delivery.getCampaign().getTitle(),
                delivery.getCampaign().getBody(),
                delivery.getCampaign().getPriority(),
                delivery.getCampaign().getOfferId(),
                delivery.getCampaign().getSourceEventId(),
                delivery.getStatus(),
                delivery.isPreferenceBypassed(),
                delivery.getSentAt(),
                delivery.getReadAt(),
                delivery.getCreatedAt()
        );
    }
}
