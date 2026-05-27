package com.ctn.offerwall.offer.eligibility.dto;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;

import java.util.List;
import java.util.UUID;

public record OfferEligibilityCheckRequest(
        UUID offerId,
        OfferEligibilityMode eligibilityMode,
        List<UUID> targetCardProductIds,
        List<String> targetIssuers,
        List<CardNetwork> targetNetworks,
        Integer targetTier,
        List<CardType> targetTypes,
        Boolean targetPersonal
) {

    public OfferEligibilityCheckRequest {
        targetCardProductIds = targetCardProductIds == null ? List.of() : List.copyOf(targetCardProductIds);
        targetIssuers = targetIssuers == null ? List.of() : List.copyOf(targetIssuers);
        targetNetworks = targetNetworks == null ? List.of() : List.copyOf(targetNetworks);
        targetTypes = targetTypes == null ? List.of() : List.copyOf(targetTypes);
    }
}
