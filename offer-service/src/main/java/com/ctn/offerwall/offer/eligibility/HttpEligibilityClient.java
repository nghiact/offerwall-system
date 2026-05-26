package com.ctn.offerwall.offer.eligibility;

import com.ctn.offerwall.offer.config.EligibilityClientProperties;
import com.ctn.offerwall.offer.eligibility.dto.BulkOfferEligibilityRequest;
import com.ctn.offerwall.offer.eligibility.dto.BulkOfferEligibilityResponse;
import com.ctn.offerwall.offer.eligibility.dto.OfferEligibilityRequest;
import com.ctn.offerwall.offer.eligibility.dto.OfferEligibilityResponse;
import com.ctn.offerwall.offer.eligibility.dto.OfferEligibilityResult;
import com.ctn.offerwall.offer.exception.UpstreamServiceException;
import com.ctn.offerwall.offer.security.InternalServiceHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Component
public class HttpEligibilityClient implements EligibilityClient {

    private final EligibilityClientProperties properties;
    private final RestClient restClient;

    public HttpEligibilityClient(EligibilityClientProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public List<UUID> resolveEligibleUsers(OfferEligibilityRequest request) {
        try {
            OfferEligibilityResponse response = restClient.post()
                    .uri("/internal/eligibility/offers/users")
                    .header(InternalServiceHeaders.INTERNAL_SERVICE_KEY_HEADER, headerValue(properties.getInternalApiKey()))
                    .body(request)
                    .retrieve()
                    .body(OfferEligibilityResponse.class);
            return response == null ? List.of() : response.eligibleUserIds();
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Offer eligibility resolution failed.", exception);
        }
    }

    @Override
    public List<OfferEligibilityResult> resolveEligibleOffers(BulkOfferEligibilityRequest request) {
        try {
            BulkOfferEligibilityResponse response = restClient.post()
                    .uri("/internal/eligibility/offers/bulk-users")
                    .header(InternalServiceHeaders.INTERNAL_SERVICE_KEY_HEADER, headerValue(properties.getInternalApiKey()))
                    .body(request)
                    .retrieve()
                    .body(BulkOfferEligibilityResponse.class);
            return response == null ? List.of() : response.offers();
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Bulk offer eligibility resolution failed.", exception);
        }
    }

    private String headerValue(String value) {
        return value == null ? "" : value;
    }
}
