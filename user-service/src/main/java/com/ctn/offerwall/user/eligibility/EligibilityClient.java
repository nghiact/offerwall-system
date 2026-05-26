package com.ctn.offerwall.user.eligibility;

import com.ctn.offerwall.user.eligibility.dto.OfferEligibilityRequest;

import java.util.List;
import java.util.UUID;

public interface EligibilityClient {

    List<UUID> resolveEligibleUsers(OfferEligibilityRequest request);
}
