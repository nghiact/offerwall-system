package com.ctn.offerwall.tracking.tracking;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventMetadata;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.tracking.config.TrackingProperties;
import com.ctn.offerwall.tracking.domain.TrackedBusinessEvent;
import com.ctn.offerwall.tracking.exception.DuplicateEventException;
import com.ctn.offerwall.tracking.exception.EventNotFoundException;
import com.ctn.offerwall.tracking.exception.UserInputException;
import com.ctn.offerwall.tracking.repository.TrackedBusinessEventRepository;
import com.ctn.offerwall.tracking.tracking.dto.BusinessEventRequest;
import com.ctn.offerwall.tracking.tracking.dto.BusinessEventResponse;
import com.ctn.offerwall.tracking.tracking.dto.RetentionPolicyResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TrackingService {

    private final TrackedBusinessEventRepository eventRepository;
    private final TrackingProperties properties;
    private final Clock clock = Clock.systemUTC();

    public TrackingService(TrackedBusinessEventRepository eventRepository, TrackingProperties properties) {
        this.eventRepository = eventRepository;
        this.properties = properties;
    }

    @Transactional
    public BusinessEventResponse recordEvent(BusinessEventRequest request) {
        BusinessEvent event = toBusinessEvent(request);
        if (eventRepository.existsById(event.eventId())) {
            throw new DuplicateEventException("Business event already exists.");
        }
        return BusinessEventResponse.from(eventRepository.save(new TrackedBusinessEvent(event)));
    }

    @Transactional(readOnly = true)
    public List<BusinessEventResponse> listEvents(EventType eventType,
                                                  EventOutcome outcome,
                                                  EntityType entityType,
                                                  String entityId,
                                                  String actorUserId,
                                                  Instant occurredFrom,
                                                  Instant occurredTo,
                                                  Integer limit) {
        validateTimeRange(occurredFrom, occurredTo);

        Specification<TrackedBusinessEvent> spec = Specification.allOf(
                equalsValue("eventType", eventType),
                equalsValue("outcome", outcome),
                equalsValue("entityType", entityType),
                equalsValue("entityId", trimToNull(entityId)),
                equalsValue("actorUserId", trimToNull(actorUserId)),
                occurredAtGreaterThanOrEqualTo(occurredFrom),
                occurredAtLessThanOrEqualTo(occurredTo)
        );

        int pageSize = normalizeLimit(limit);
        return eventRepository.findAll(spec, PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "occurredAt")))
                .stream()
                .map(BusinessEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessEventResponse getEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .map(BusinessEventResponse::from)
                .orElseThrow(() -> new EventNotFoundException("Business event was not found."));
    }

    public RetentionPolicyResponse retentionPolicy() {
        return new RetentionPolicyResponse(
                properties.getRetentionDays(),
                Instant.now(clock).minus(properties.getRetentionDays(), ChronoUnit.DAYS)
        );
    }

    private BusinessEvent toBusinessEvent(BusinessEventRequest request) {
        return new BusinessEvent(
                request.eventId(),
                request.eventType(),
                request.outcome(),
                request.entityType(),
                trimToNull(request.entityId()),
                trimToNull(request.actorUserId()),
                request.occurredAt(),
                new EventMetadata(metadataOrEmpty(request.metadata()))
        );
    }

    private Map<String, String> metadataOrEmpty(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = trimToNull(entry.getKey());
            String value = trimToNull(entry.getValue());
            if (key == null || value == null) {
                throw new UserInputException("Metadata keys and values must not be blank.");
            }
            normalized.put(key, value);
        }
        return normalized;
    }

    private void validateTimeRange(Instant occurredFrom, Instant occurredTo) {
        if (occurredFrom != null && occurredTo != null && occurredTo.isBefore(occurredFrom)) {
            throw new UserInputException("occurredTo must be after occurredFrom.");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return properties.getDefaultPageSize();
        }
        if (limit < 1) {
            throw new UserInputException("limit must be at least 1.");
        }
        return Math.min(limit, properties.getMaxPageSize());
    }

    private Specification<TrackedBusinessEvent> equalsValue(String field, Object value) {
        return value == null ? null : (root, query, builder) -> builder.equal(root.get(field), value);
    }

    private Specification<TrackedBusinessEvent> occurredAtGreaterThanOrEqualTo(Instant occurredFrom) {
        return occurredFrom == null ? null : (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("occurredAt"), occurredFrom);
    }

    private Specification<TrackedBusinessEvent> occurredAtLessThanOrEqualTo(Instant occurredTo) {
        return occurredTo == null ? null : (root, query, builder) -> builder.lessThanOrEqualTo(root.get("occurredAt"), occurredTo);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
