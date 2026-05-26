package com.ctn.offerwall.eligibility.eligibility;

import com.ctn.offerwall.eligibility.eligibility.dto.OfferEligibilityRequest;
import com.ctn.offerwall.eligibility.eligibility.dto.OfferEligibilityResponse;
import com.ctn.offerwall.eligibility.security.InternalSecurityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/eligibility")
public class OfferEligibilityController {

    private final OfferEligibilityService eligibilityService;
    private final InternalSecurityService securityService;

    public OfferEligibilityController(OfferEligibilityService eligibilityService,
                                      InternalSecurityService securityService) {
        this.eligibilityService = eligibilityService;
        this.securityService = securityService;
    }

    @PostMapping("/offers/users")
    public OfferEligibilityResponse resolveUsers(
            @RequestHeader(name = InternalSecurityService.INTERNAL_SERVICE_KEY_HEADER, required = false) String internalServiceKey,
            @Valid @RequestBody OfferEligibilityRequest request) {
        securityService.requireInternalServiceKey(internalServiceKey);
        return eligibilityService.resolveUsers(request);
    }
}
