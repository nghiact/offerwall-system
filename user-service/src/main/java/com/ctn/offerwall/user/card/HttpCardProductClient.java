package com.ctn.offerwall.user.card;

import com.ctn.offerwall.user.card.dto.CardProductLookupRequest;
import com.ctn.offerwall.user.card.dto.CardProductSummaryResponse;
import com.ctn.offerwall.user.config.CardClientProperties;
import com.ctn.offerwall.user.exception.UpstreamServiceException;
import com.ctn.offerwall.user.security.InternalSecurityService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
    public List<CardProductSummaryResponse> lookupProducts(List<UUID> cardProductIds) {
        try {
            List<CardProductSummaryResponse> response = restClient.post()
                    .uri("/internal/card-products/lookup")
                    .header(InternalSecurityService.INTERNAL_SERVICE_KEY_HEADER, headerValue(properties.getInternalApiKey()))
                    .body(new CardProductLookupRequest(cardProductIds))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return response == null ? List.of() : response;
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("Card product lookup failed.", exception);
        }
    }

    private String headerValue(String value) {
        return value == null ? "" : value;
    }
}
