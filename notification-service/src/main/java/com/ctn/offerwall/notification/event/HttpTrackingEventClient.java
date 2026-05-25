package com.ctn.offerwall.notification.event;

import com.ctn.offerwall.notification.config.NotificationClientProperties;
import com.ctn.offerwall.notification.exception.UserInputException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class HttpTrackingEventClient implements TrackingEventClient {

    private final RestClient restClient;

    public HttpTrackingEventClient(RestClient.Builder restClientBuilder,
                                   NotificationClientProperties properties) {
        this.restClient = restClientBuilder
                .clone()
                .baseUrl(properties.getTracking().getBaseUrl())
                .build();
    }

    @Override
    public BusinessEventSnapshot getEvent(UUID eventId) {
        try {
            return restClient.get()
                    .uri("/api/tracking/events/{eventId}", eventId)
                    .retrieve()
                    .body(BusinessEventSnapshot.class);
        } catch (RestClientException exception) {
            throw new UserInputException("Tracking event could not be loaded.");
        }
    }
}
