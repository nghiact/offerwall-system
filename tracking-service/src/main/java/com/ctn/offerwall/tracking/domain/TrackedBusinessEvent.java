package com.ctn.offerwall.tracking.domain;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventMetadata;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "business_events",
        indexes = {
                @Index(name = "idx_business_events_event_type", columnList = "event_type"),
                @Index(name = "idx_business_events_outcome", columnList = "outcome"),
                @Index(name = "idx_business_events_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_business_events_actor_user_id", columnList = "actor_user_id"),
                @Index(name = "idx_business_events_occurred_at", columnList = "occurred_at")
        }
)
public class TrackedBusinessEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 40)
    private EntityType entityType;

    @Column(name = "entity_id", length = 120)
    private String entityId;

    @Column(name = "actor_user_id", length = 120)
    private String actorUserId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @ElementCollection
    @CollectionTable(
            name = "business_event_metadata",
            joinColumns = @JoinColumn(name = "event_id"),
            indexes = @Index(name = "idx_business_event_metadata_event_id", columnList = "event_id")
    )
    @MapKeyColumn(name = "metadata_key", length = 120)
    @Column(name = "metadata_value", length = 1000)
    private Map<String, String> metadata = new LinkedHashMap<>();

    protected TrackedBusinessEvent() {
    }

    public TrackedBusinessEvent(BusinessEvent event) {
        this.eventId = event.eventId();
        this.eventType = event.eventType();
        this.outcome = event.outcome();
        this.entityType = event.entityType();
        this.entityId = event.entityId();
        this.actorUserId = event.actorUserId();
        this.occurredAt = event.occurredAt();
        this.metadata.putAll(event.metadata().values());
    }

    public BusinessEvent toBusinessEvent() {
        return new BusinessEvent(
                eventId,
                eventType,
                outcome,
                entityType,
                entityId,
                actorUserId,
                occurredAt,
                new EventMetadata(metadata)
        );
    }

    @PrePersist
    void prePersist() {
        this.receivedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventOutcome getOutcome() {
        return outcome;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
