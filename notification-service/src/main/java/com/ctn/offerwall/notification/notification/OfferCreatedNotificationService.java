package com.ctn.offerwall.notification.notification;

import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.notification.event.BusinessEventSnapshot;
import com.ctn.offerwall.notification.event.TrackingEventClient;
import com.ctn.offerwall.notification.exception.UserInputException;
import com.ctn.offerwall.notification.notification.dto.NotificationCampaignResponse;
import com.ctn.offerwall.notification.notification.dto.NotificationRecipientRequest;
import com.ctn.offerwall.notification.notification.dto.NotificationRequest;
import com.ctn.offerwall.notification.notification.dto.OfferCreatedNotificationRequest;
import com.ctn.offerwall.notification.offer.OfferLookupClient;
import com.ctn.offerwall.notification.offer.OfferSnapshot;
import com.ctn.offerwall.notification.recipient.OfferNotificationRecipientQuery;
import com.ctn.offerwall.notification.recipient.OfferNotificationRecipientResolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OfferCreatedNotificationService {

    private final TrackingEventClient trackingEventClient;
    private final OfferLookupClient offerLookupClient;
    private final OfferNotificationRecipientResolver recipientResolver;
    private final NotificationService notificationService;

    public OfferCreatedNotificationService(TrackingEventClient trackingEventClient,
                                           OfferLookupClient offerLookupClient,
                                           OfferNotificationRecipientResolver recipientResolver,
                                           NotificationService notificationService) {
        this.trackingEventClient = trackingEventClient;
        this.offerLookupClient = offerLookupClient;
        this.recipientResolver = recipientResolver;
        this.notificationService = notificationService;
    }

    public NotificationCampaignResponse createFromOfferCreatedEvent(UUID eventId,
                                                                    OfferCreatedNotificationRequest request) {
        BusinessEventSnapshot event = trackingEventClient.getEvent(eventId);
        validateOfferCreatedEvent(event, eventId);

        UUID offerId = parseOfferId(event.entityId());
        OfferSnapshot offer = offerLookupClient.getOffer(offerId);
        if (offer == null || !offerId.equals(offer.id())) {
            throw new UserInputException("Offer lookup returned an unexpected offer.");
        }

        List<NotificationRecipientRequest> recipients = recipientResolver
                .resolveRecipients(OfferNotificationRecipientQuery.from(event.eventId(), offer))
                .stream()
                .map(candidate -> new NotificationRecipientRequest(
                        candidate.userId(),
                        candidate.email(),
                        candidate.emailEnabled(),
                        candidate.inAppEnabled()
                ))
                .toList();

        if (recipients.isEmpty()) {
            throw new UserInputException("Offer notification has no eligible recipients.");
        }

        return notificationService.createNotification(new NotificationRequest(
                title(request, offer),
                body(request, offer),
                request.priority(),
                request.sendMode(),
                request.scheduledFor(),
                offer.id(),
                event.eventId(),
                request.channels(),
                recipients
        ));
    }

    private void validateOfferCreatedEvent(BusinessEventSnapshot event, UUID requestedEventId) {
        if (event == null || !requestedEventId.equals(event.eventId())) {
            throw new UserInputException("Tracking event could not be loaded.");
        }
        if (event.eventType() != EventType.OFFER_CREATED
                || event.outcome() != EventOutcome.SUCCESS
                || event.entityType() != EntityType.OFFER) {
            throw new UserInputException("Tracking event is not a successful OFFER_CREATED event.");
        }
    }

    private UUID parseOfferId(String entityId) {
        try {
            return UUID.fromString(entityId);
        } catch (IllegalArgumentException exception) {
            throw new UserInputException("Tracking event entityId is not a valid offer ID.");
        }
    }

    private String title(OfferCreatedNotificationRequest request, OfferSnapshot offer) {
        String title = trimToNull(request.title());
        return title == null ? "New offer: " + offer.merchantName() : title;
    }

    private String body(OfferCreatedNotificationRequest request, OfferSnapshot offer) {
        String body = trimToNull(request.body());
        return body == null ? offer.offerSummary() : body;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
