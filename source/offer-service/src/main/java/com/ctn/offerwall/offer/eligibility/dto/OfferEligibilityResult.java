package com.ctn.offerwall.offer.eligibility.dto;

import java.util.List;
import java.util.UUID;

public record OfferEligibilityResult(
        UUID offerId,
        List<UUID> eligibleUserIds
) {

    public OfferEligibilityResult {
        eligibleUserIds = eligibleUserIds == null ? List.of() : List.copyOf(eligibleUserIds);
    }
}
