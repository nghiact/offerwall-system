package com.ctn.offerwall.notification.notification.dto;

import com.ctn.offerwall.common.notification.NotificationChannel;
import com.ctn.offerwall.notification.domain.NotificationDelivery;
import com.ctn.offerwall.notification.domain.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryResponse(
        UUID id,
        UUID campaignId,
        UUID recipientUserId,
        String recipientEmail,
        NotificationChannel channel,
        NotificationDeliveryStatus status,
        boolean preferenceBypassed,
        String failureReason,
        Instant sentAt,
        Instant readAt,
        Instant createdAt
) {

    public static NotificationDeliveryResponse from(NotificationDelivery delivery) {
        return new NotificationDeliveryResponse(
                delivery.getId(),
                delivery.getCampaign().getId(),
                delivery.getRecipientUserId(),
                delivery.getRecipientEmail(),
                delivery.getChannel(),
                delivery.getStatus(),
                delivery.isPreferenceBypassed(),
                delivery.getFailureReason(),
                delivery.getSentAt(),
                delivery.getReadAt(),
                delivery.getCreatedAt()
        );
    }
}
