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
        List<String> targetIssuers,
        List<CardNetwork> targetNetworks,
        Integer targetTier,
        List<CardType> targetTypes,
        Boolean targetPersonal
) {

    public static OfferNotificationRecipientQuery from(UUID sourceEventId, OfferSnapshot offer) {
        return new OfferNotificationRecipientQuery(
                offer.id(),
                sourceEventId,
                offer.eligibilityMode(),
                offer.targetCardProductIds() == null ? List.of() : List.copyOf(offer.targetCardProductIds()),
                targetIssuers(offer),
                targetNetworks(offer),
                offer.targetTier(),
                targetTypes(offer),
                offer.targetPersonal()
        );
    }

    private static List<String> targetIssuers(OfferSnapshot offer) {
        if (offer.targetIssuers() != null && !offer.targetIssuers().isEmpty()) {
            return List.copyOf(offer.targetIssuers());
        }
        return offer.targetIssuer() == null || offer.targetIssuer().isBlank() ? List.of() : List.of(offer.targetIssuer());
    }

    private static List<CardNetwork> targetNetworks(OfferSnapshot offer) {
        if (offer.targetNetworks() != null && !offer.targetNetworks().isEmpty()) {
            return List.copyOf(offer.targetNetworks());
        }
        return offer.targetNetwork() == null ? List.of() : List.of(offer.targetNetwork());
    }

    private static List<CardType> targetTypes(OfferSnapshot offer) {
        if (offer.targetTypes() != null && !offer.targetTypes().isEmpty()) {
            return List.copyOf(offer.targetTypes());
        }
        return offer.targetType() == null ? List.of() : List.of(offer.targetType());
    }
}
