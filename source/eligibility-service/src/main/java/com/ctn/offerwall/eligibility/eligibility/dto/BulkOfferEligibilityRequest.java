package com.ctn.offerwall.eligibility.eligibility.dto;

import jakarta.validation.Valid;

import java.util.List;

public record BulkOfferEligibilityRequest(
        List<@Valid OfferEligibilityCheckRequest> offers,
        List<@Valid UserWalletCandidate> candidates
) {

    public BulkOfferEligibilityRequest {
        offers = offers == null ? List.of() : List.copyOf(offers);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
