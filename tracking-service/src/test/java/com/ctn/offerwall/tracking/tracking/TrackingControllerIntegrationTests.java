package com.ctn.offerwall.tracking.tracking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrackingControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recordsEventAndReadsItBack() throws Exception {
        UUID eventId = UUID.randomUUID();
        String entityId = UUID.randomUUID().toString();
        Instant occurredAt = Instant.now().minusSeconds(30);

        String response = mockMvc.perform(post("/api/tracking/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(eventId, "OFFER_CREATED", "SUCCESS", "OFFER", entityId, "admin-1", occurredAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.eventType").value("OFFER_CREATED"))
                .andExpect(jsonPath("$.outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.entityType").value("OFFER"))
                .andExpect(jsonPath("$.entityId").value(entityId))
                .andExpect(jsonPath("$.actorUserId").value("admin-1"))
                .andExpect(jsonPath("$.receivedAt", notNullValue()))
                .andExpect(jsonPath("$.metadata.source").value("integration-test"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        mockMvc.perform(get("/api/tracking/events/{eventId}", json.get("eventId").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.metadata.source").value("integration-test"));
    }

    @Test
    void filtersEvents() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String offerEntityId = "offer-" + suffix;
        String userEntityId = "user-" + suffix;

        recordEvent(UUID.randomUUID(), "OFFER_UPDATED", "SUCCESS", "OFFER", offerEntityId, "admin-2", Instant.now().minusSeconds(120));
        recordEvent(UUID.randomUUID(), "USER_LOGIN_FAILED", "FAILURE", "USER", userEntityId, null, Instant.now().minusSeconds(60));

        mockMvc.perform(get("/api/tracking/events")
                        .param("eventType", "OFFER_UPDATED")
                        .param("outcome", "SUCCESS")
                        .param("entityType", "OFFER")
                        .param("entityId", offerEntityId)
                        .param("actorUserId", "admin-2")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].entityId").value(offerEntityId));
    }

    @Test
    void rejectsDuplicateEventIdAndInvalidFilters() throws Exception {
        UUID eventId = UUID.randomUUID();
        recordEvent(eventId, "CARD_CREATED", "SUCCESS", "CARD", "card-" + eventId, "admin-3", Instant.now());

        mockMvc.perform(post("/api/tracking/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(eventId, "CARD_CREATED", "SUCCESS", "CARD", "card-" + eventId, "admin-3", Instant.now())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Business event already exists."));

        mockMvc.perform(get("/api/tracking/events")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit must be at least 1."));

        mockMvc.perform(get("/api/tracking/events")
                        .param("occurredFrom", Instant.now().toString())
                        .param("occurredTo", Instant.now().minusSeconds(60).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("occurredTo must be after occurredFrom."));
    }

    @Test
    void exposesRetentionPolicy() throws Exception {
        mockMvc.perform(get("/api/tracking/retention-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionDays").value(365))
                .andExpect(jsonPath("$.deleteBefore", notNullValue()));
    }

    private void recordEvent(UUID eventId,
                             String eventType,
                             String outcome,
                             String entityType,
                             String entityId,
                             String actorUserId,
                             Instant occurredAt) throws Exception {
        mockMvc.perform(post("/api/tracking/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(eventId, eventType, outcome, entityType, entityId, actorUserId, occurredAt)))
                .andExpect(status().isCreated());
    }

    private String eventJson(UUID eventId,
                             String eventType,
                             String outcome,
                             String entityType,
                             String entityId,
                             String actorUserId,
                             Instant occurredAt) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "outcome": "%s",
                  "entityType": "%s",
                  "entityId": "%s",
                  "actorUserId": %s,
                  "occurredAt": "%s",
                  "metadata": {
                    "source": "integration-test"
                  }
                }
                """.formatted(
                eventId,
                eventType,
                outcome,
                entityType,
                entityId,
                actorUserId == null ? "null" : "\"" + actorUserId + "\"",
                occurredAt
        );
    }
}
