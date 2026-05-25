package com.ctn.offerwall.tracking.tracking.dto;

import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BusinessEventRequest(
        UUID eventId,

        @NotNull
        EventType eventType,

        @NotNull
        EventOutcome outcome,

        @NotNull
        EntityType entityType,

        @Size(max = 120)
        String entityId,

        @Size(max = 120)
        String actorUserId,

        Instant occurredAt,

        @Size(max = 50)
        Map<@Size(max = 120) String, @Size(max = 1000) String> metadata
) {
}
