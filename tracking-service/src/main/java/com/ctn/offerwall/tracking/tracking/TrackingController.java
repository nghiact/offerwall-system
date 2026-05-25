package com.ctn.offerwall.tracking.tracking;

import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.tracking.tracking.dto.BusinessEventRequest;
import com.ctn.offerwall.tracking.tracking.dto.BusinessEventResponse;
import com.ctn.offerwall.tracking.tracking.dto.RetentionPolicyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessEventResponse record(@Valid @RequestBody BusinessEventRequest request) {
        return trackingService.recordEvent(request);
    }

    @GetMapping("/events")
    public List<BusinessEventResponse> list(@RequestParam(required = false) EventType eventType,
                                            @RequestParam(required = false) EventOutcome outcome,
                                            @RequestParam(required = false) EntityType entityType,
                                            @RequestParam(required = false) String entityId,
                                            @RequestParam(required = false) String actorUserId,
                                            @RequestParam(required = false) Instant occurredFrom,
                                            @RequestParam(required = false) Instant occurredTo,
                                            @RequestParam(required = false) Integer limit) {
        return trackingService.listEvents(eventType, outcome, entityType, entityId, actorUserId, occurredFrom, occurredTo, limit);
    }

    @GetMapping("/events/{eventId}")
    public BusinessEventResponse get(@PathVariable UUID eventId) {
        return trackingService.getEvent(eventId);
    }

    @GetMapping("/retention-policy")
    public RetentionPolicyResponse retentionPolicy() {
        return trackingService.retentionPolicy();
    }
}
