package com.ctn.offerwall.offer.eligibility.dto;

import java.util.List;

public record BulkOfferEligibilityResponse(
        List<OfferEligibilityResult> offers
) {

    public BulkOfferEligibilityResponse {
        offers = offers == null ? List.of() : List.copyOf(offers);
    }
}
