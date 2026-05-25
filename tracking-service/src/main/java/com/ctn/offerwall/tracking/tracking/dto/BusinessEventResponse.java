package com.ctn.offerwall.tracking.tracking.dto;

import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.tracking.domain.TrackedBusinessEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BusinessEventResponse(
        UUID eventId,
        EventType eventType,
        EventOutcome outcome,
        EntityType entityType,
        String entityId,
        String actorUserId,
        Instant occurredAt,
        Instant receivedAt,
        Map<String, String> metadata
) {

    public static BusinessEventResponse from(TrackedBusinessEvent event) {
        return new BusinessEventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getOutcome(),
                event.getEntityType(),
                event.getEntityId(),
                event.getActorUserId(),
                event.getOccurredAt(),
                event.getReceivedAt(),
                Map.copyOf(event.getMetadata())
        );
    }
}
