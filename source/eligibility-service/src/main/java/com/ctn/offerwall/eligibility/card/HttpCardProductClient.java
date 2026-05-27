package com.ctn.offerwall.eligibility.card;

import com.ctn.offerwall.eligibility.config.CardClientProperties;
import com.ctn.offerwall.eligibility.exception.UpstreamServiceException;
import com.ctn.offerwall.eligibility.security.InternalSecurityService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class HttpCardProductClient implements CardProductClient {

    private final CardClientProperties properties;
    private final RestClient restClient;

    public HttpCardProductClient(CardClientProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public List<CardProductSummary> lookupProducts(List<UUID> cardProductIds) {
        if (cardProductIds == null || cardProductIds.isEmpty()) {
            return List.of();
        }

        try {
            CardProductSummary[] response = restClient.post()
                    .uri("/internal/card-products/lookup")
                    .header(InternalSecurityService.INTERNAL_SERVICE_KEY_HEADER, headerValue(properties.getInternalApiKey()))
                    .body(new CardProductLookupRequest(cardProductIds))
                    .retrieve()
                    .body(CardProductSummary[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Card product lookup failed.", exception);
        }
    }

    private String headerValue(String value) {
        return value == null ? "" : value;
    }
}
