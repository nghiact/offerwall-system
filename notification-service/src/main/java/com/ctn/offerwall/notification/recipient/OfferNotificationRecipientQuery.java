package com.ctn.offerwall.notification.recipient;

import java.util.UUID;

public record OfferNotificationRecipientQuery(
        UUID offerId,
        UUID sourceEventId
) {
}
