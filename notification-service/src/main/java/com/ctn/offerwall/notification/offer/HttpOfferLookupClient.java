package com.ctn.offerwall.notification.offer;

import com.ctn.offerwall.notification.config.NotificationClientProperties;
import com.ctn.offerwall.notification.exception.UserInputException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class HttpOfferLookupClient implements OfferLookupClient {

    private final RestClient restClient;

    public HttpOfferLookupClient(RestClient.Builder restClientBuilder,
                                 NotificationClientProperties properties) {
        this.restClient = restClientBuilder
                .clone()
                .baseUrl(properties.getOffer().getBaseUrl())
                .build();
    }

    @Override
    public OfferSnapshot getOffer(UUID offerId) {
        try {
            return restClient.get()
                    .uri("/api/offers/{offerId}", offerId)
                    .retrieve()
                    .body(OfferSnapshot.class);
        } catch (RestClientException exception) {
            throw new UserInputException("Offer could not be loaded.");
        }
    }
}
