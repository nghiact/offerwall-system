package com.ctn.offerwall.notification.notification.dto;

import com.ctn.offerwall.common.notification.NotificationPriority;
import com.ctn.offerwall.notification.domain.NotificationCampaign;
import com.ctn.offerwall.notification.domain.NotificationDelivery;
import com.ctn.offerwall.notification.domain.NotificationSendMode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotificationCampaignResponse(
        UUID id,
        String title,
        String body,
        NotificationPriority priority,
        NotificationSendMode sendMode,
        Instant scheduledFor,
        UUID offerId,
        UUID sourceEventId,
        Instant createdAt,
        Instant updatedAt,
        int recipientCount,
        int deliveryCount,
        List<NotificationDeliveryResponse> deliveries
) {

    public static NotificationCampaignResponse from(NotificationCampaign campaign) {
        List<NotificationDeliveryResponse> deliveries = campaign.getDeliveries().stream()
                .map(NotificationDeliveryResponse::from)
                .toList();
        int recipientCount = (int) campaign.getDeliveries().stream()
                .map(NotificationDelivery::getRecipientUserId)
                .distinct()
                .count();
        return new NotificationCampaignResponse(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getBody(),
                campaign.getPriority(),
                campaign.getSendMode(),
                campaign.getScheduledFor(),
                campaign.getOfferId(),
                campaign.getSourceEventId(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt(),
                recipientCount,
                deliveries.size(),
                deliveries
        );
    }
}
