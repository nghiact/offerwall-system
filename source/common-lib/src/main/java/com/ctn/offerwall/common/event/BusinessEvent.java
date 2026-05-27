package com.ctn.offerwall.common.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BusinessEvent(
        UUID eventId,
        EventType eventType,
        EventOutcome outcome,
        EntityType entityType,
        String entityId,
        String actorUserId,
        Instant occurredAt,
        EventMetadata metadata
) {

    public BusinessEvent {
        eventId = Objects.requireNonNullElseGet(eventId, UUID::randomUUID);
        eventType = Objects.requireNonNull(eventType, "eventType is required");
        outcome = Objects.requireNonNull(outcome, "outcome is required");
        entityType = Objects.requireNonNull(entityType, "entityType is required");
        occurredAt = Objects.requireNonNullElseGet(occurredAt, Instant::now);
        metadata = metadata == null ? EventMetadata.empty() : metadata;
    }

    public static BusinessEvent success(EventType eventType, EntityType entityType, String entityId,
                                        String actorUserId, EventMetadata metadata) {
        return new BusinessEvent(null, eventType, EventOutcome.SUCCESS, entityType, entityId,
                actorUserId, null, metadata);
    }

    public static BusinessEvent failure(EventType eventType, EntityType entityType, String entityId,
                                        String actorUserId, EventMetadata metadata) {
        return new BusinessEvent(null, eventType, EventOutcome.FAILURE, entityType, entityId,
                actorUserId, null, metadata);
    }
}
