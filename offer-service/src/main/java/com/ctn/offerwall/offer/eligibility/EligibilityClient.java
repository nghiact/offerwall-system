package com.ctn.offerwall.offer.eligibility;

import com.ctn.offerwall.offer.eligibility.dto.OfferEligibilityRequest;

import java.util.List;
import java.util.UUID;

public interface EligibilityClient {

    List<UUID> resolveEligibleUsers(OfferEligibilityRequest request);
}
