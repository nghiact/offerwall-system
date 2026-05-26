package com.ctn.offerwall.offer.eligibility.dto;

import java.util.List;
import java.util.UUID;

public record OfferEligibilityResponse(
        List<UUID> eligibleUserIds
) {

    public OfferEligibilityResponse {
        eligibleUserIds = eligibleUserIds == null ? List.of() : List.copyOf(eligibleUserIds);
    }
}
