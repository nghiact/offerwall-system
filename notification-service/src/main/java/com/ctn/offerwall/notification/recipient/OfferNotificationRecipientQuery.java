package com.ctn.offerwall.notification.recipient;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.notification.offer.OfferSnapshot;

import java.util.List;
import java.util.UUID;

public record OfferNotificationRecipientQuery(
        UUID offerId,
        UUID sourceEventId,
        OfferEligibilityMode eligibilityMode,
        List<UUID> targetCardProductIds,
        String targetIssuer,
        CardNetwork targetNetwork,
        Integer targetTier,
        CardType targetType,
        Boolean targetPersonal
) {

    public static OfferNotificationRecipientQuery from(UUID sourceEventId, OfferSnapshot offer) {
        return new OfferNotificationRecipientQuery(
                offer.id(),
                sourceEventId,
                offer.eligibilityMode(),
                offer.targetCardProductIds() == null ? List.of() : List.copyOf(offer.targetCardProductIds()),
                offer.targetIssuer(),
                offer.targetNetwork(),
                offer.targetTier(),
                offer.targetType(),
                offer.targetPersonal()
        );
    }
}
