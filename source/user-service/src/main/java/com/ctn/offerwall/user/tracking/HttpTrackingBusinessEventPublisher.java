package com.ctn.offerwall.user.tracking;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.user.config.UserTrackingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "offerwall.tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HttpTrackingBusinessEventPublisher implements BusinessEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(HttpTrackingBusinessEventPublisher.class);

    private final UserTrackingProperties properties;
    private final RestClient restClient;

    public HttpTrackingBusinessEventPublisher(UserTrackingProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public void publish(BusinessEvent event) {
        try {
            restClient.post()
                    .uri("/api/tracking/events")
                    .body(BusinessEventPayload.from(event))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            if (properties.isFailOnError()) {
                throw exception;
            }
            log.warn("Business event was not published to tracking-service: {}", event.eventId(), exception);
        }
    }

    private record BusinessEventPayload(
            UUID eventId,
            String eventType,
            String outcome,
            String entityType,
            String entityId,
            String actorUserId,
            Instant occurredAt,
            Map<String, String> metadata
    ) {

        private static BusinessEventPayload from(BusinessEvent event) {
            return new BusinessEventPayload(
                    event.eventId(),
                    event.eventType().name(),
                    event.outcome().name(),
                    event.entityType().name(),
                    event.entityId(),
                    event.actorUserId(),
                    event.occurredAt(),
                    event.metadata().values()
            );
        }
    }
}
