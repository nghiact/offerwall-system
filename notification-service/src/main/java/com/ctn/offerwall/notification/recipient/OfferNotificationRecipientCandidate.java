package com.ctn.offerwall.notification.recipient;

import com.ctn.offerwall.notification.notification.dto.NotificationRecipientRequest;

import java.util.UUID;

public record OfferNotificationRecipientCandidate(
        UUID userId,
        String email,
        boolean emailEnabled,
        boolean inAppEnabled
) {

    public NotificationRecipientRequest toNotificationRecipientRequest() {
        return new NotificationRecipientRequest(userId, email, emailEnabled, inAppEnabled);
    }
}
