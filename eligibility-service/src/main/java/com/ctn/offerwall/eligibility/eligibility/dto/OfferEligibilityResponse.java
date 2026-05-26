package com.ctn.offerwall.eligibility.eligibility.dto;

import java.util.List;
import java.util.UUID;

public record OfferEligibilityResponse(
        List<UUID> eligibleUserIds
) {

    public OfferEligibilityResponse {
        eligibleUserIds = eligibleUserIds == null ? List.of() : List.copyOf(eligibleUserIds);
    }
}
