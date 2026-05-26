package com.ctn.offerwall.eligibility.eligibility.dto;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OfferEligibilityRequest(
        @NotNull
        OfferEligibilityMode eligibilityMode,

        List<UUID> targetCardProductIds,

        List<String> targetIssuers,

        List<CardNetwork> targetNetworks,

        Integer targetTier,

        List<CardType> targetTypes,

        Boolean targetPersonal,

        List<@Valid UserWalletCandidate> candidates
) {

    public OfferEligibilityRequest {
        targetCardProductIds = targetCardProductIds == null ? List.of() : List.copyOf(targetCardProductIds);
        targetIssuers = targetIssuers == null ? List.of() : List.copyOf(targetIssuers);
        targetNetworks = targetNetworks == null ? List.of() : List.copyOf(targetNetworks);
        targetTypes = targetTypes == null ? List.of() : List.copyOf(targetTypes);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
