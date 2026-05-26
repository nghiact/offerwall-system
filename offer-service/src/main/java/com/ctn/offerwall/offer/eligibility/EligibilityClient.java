package com.ctn.offerwall.offer.eligibility;

import com.ctn.offerwall.offer.eligibility.dto.OfferEligibilityRequest;
import com.ctn.offerwall.offer.eligibility.dto.OfferEligibilityResult;
import com.ctn.offerwall.offer.eligibility.dto.BulkOfferEligibilityRequest;

import java.util.List;
import java.util.UUID;

public interface EligibilityClient {

    List<UUID> resolveEligibleUsers(OfferEligibilityRequest request);

    List<OfferEligibilityResult> resolveEligibleOffers(BulkOfferEligibilityRequest request);
}
