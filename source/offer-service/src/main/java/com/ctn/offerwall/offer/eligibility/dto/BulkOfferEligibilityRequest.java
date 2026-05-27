package com.ctn.offerwall.offer.eligibility.dto;

import java.util.List;

public record BulkOfferEligibilityRequest(
        List<OfferEligibilityCheckRequest> offers,
        List<UserWalletCandidate> candidates
) {

    public BulkOfferEligibilityRequest {
        offers = offers == null ? List.of() : List.copyOf(offers);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
