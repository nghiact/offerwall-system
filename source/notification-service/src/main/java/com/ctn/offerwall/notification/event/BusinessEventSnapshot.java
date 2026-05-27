package com.ctn.offerwall.notification.event;

import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BusinessEventSnapshot(
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
}
