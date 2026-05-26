package com.ctn.offerwall.notification.notification;

import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.common.notification.NotificationPriority;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;
import com.ctn.offerwall.notification.event.BusinessEventSnapshot;
import com.ctn.offerwall.notification.event.TrackingEventClient;
import com.ctn.offerwall.notification.offer.OfferLookupClient;
import com.ctn.offerwall.notification.offer.OfferSnapshot;
import com.ctn.offerwall.notification.recipient.OfferNotificationRecipientCandidate;
import com.ctn.offerwall.notification.recipient.OfferNotificationRecipientResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "offerwall.security.internal.api-key=stage-8-internal")
@AutoConfigureMockMvc
@Transactional
class OfferCreatedNotificationControllerIntegrationTests {

    private static final String ADMIN_AUTHORIZATION = "Bearer " + accessToken("ADMIN", UUID.randomUUID());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrackingEventClient trackingEventClient;

    @MockitoBean
    private OfferLookupClient offerLookupClient;

    @MockitoBean
    private OfferNotificationRecipientResolver recipientResolver;

    @Test
    void createsNotificationFromOfferCreatedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID targetCardProductId = UUID.randomUUID();
        stubOfferCreatedFlow(eventId, offerId, targetCardProductId, userId);

        String response = mockMvc.perform(post("/api/notifications/offer-created-events/{eventId}", eventId)
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("NORMAL", "IMMEDIATE", null, "[\"IN_APP\", \"EMAIL\"]", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New offer: Stage 8 Merchant"))
                .andExpect(jsonPath("$.body").value("Stage 8 offer summary"))
                .andExpect(jsonPath("$.offerId").value(offerId.toString()))
                .andExpect(jsonPath("$.sourceEventId").value(eventId.toString()))
                .andExpect(jsonPath("$.recipientCount").value(1))
                .andExpect(jsonPath("$.deliveryCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode campaign = objectMapper.readTree(response);
        assertThat(campaign.get("id").asText()).isNotBlank();
    }

    @Test
    void sourceEventIdKeepsOfferCreatedNotificationIdempotent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID targetCardProductId = UUID.randomUUID();
        stubOfferCreatedFlow(eventId, offerId, targetCardProductId, userId);

        String firstResponse = mockMvc.perform(post("/api/notifications/offer-created-events/{eventId}", eventId)
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("NORMAL", "IMMEDIATE", null, "[\"IN_APP\"]", "Custom title", "Custom body")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/notifications/offer-created-events/{eventId}", eventId)
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("NORMAL", "IMMEDIATE", null, "[\"IN_APP\"]", "Changed title", "Changed body")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstResponse);
        JsonNode second = objectMapper.readTree(secondResponse);
        assertThat(second.get("id").asText()).isEqualTo(first.get("id").asText());
        assertThat(second.get("title").asText()).isEqualTo("Custom title");
        assertThat(second.get("deliveryCount").asInt()).isEqualTo(1);
    }

    @Test
    void rejectsNonOfferCreatedEventAndNoRecipients() throws Exception {
        UUID badEventId = UUID.randomUUID();
        when(trackingEventClient.getEvent(badEventId)).thenReturn(new BusinessEventSnapshot(
                badEventId,
                EventType.OFFER_UPDATED,
                EventOutcome.SUCCESS,
                EntityType.OFFER,
                UUID.randomUUID().toString(),
                "admin",
                Instant.now(),
                Instant.now(),
                Map.of()
        ));

        mockMvc.perform(post("/api/notifications/offer-created-events/{eventId}", badEventId)
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("NORMAL", "IMMEDIATE", null, "[\"IN_APP\"]", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tracking event is not a successful OFFER_CREATED event."));

        UUID emptyEventId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        when(trackingEventClient.getEvent(emptyEventId)).thenReturn(successfulOfferCreatedEvent(emptyEventId, offerId));
        when(offerLookupClient.getOffer(offerId)).thenReturn(offer(offerId, List.of()));
        when(recipientResolver.resolveRecipients(argThat(query -> query.offerId().equals(offerId)))).thenReturn(List.of());

        mockMvc.perform(post("/api/notifications/offer-created-events/{eventId}", emptyEventId)
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("NORMAL", "IMMEDIATE", null, "[\"IN_APP\"]", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Offer notification has no eligible recipients."));
    }

    private void stubOfferCreatedFlow(UUID eventId, UUID offerId, UUID targetCardProductId, UUID userId) {
        when(trackingEventClient.getEvent(eventId)).thenReturn(successfulOfferCreatedEvent(eventId, offerId));
        when(offerLookupClient.getOffer(offerId)).thenReturn(offer(offerId, List.of(targetCardProductId)));
        when(recipientResolver.resolveRecipients(argThat(query ->
                query.offerId().equals(offerId)
                        && query.sourceEventId().equals(eventId)
                        && query.eligibilityMode() == OfferEligibilityMode.CARD_IDS
                        && query.targetCardProductIds().contains(targetCardProductId)
        ))).thenReturn(List.of(new OfferNotificationRecipientCandidate(
                userId,
                "stage8@example.com",
                true,
                true
        )));
    }

    private BusinessEventSnapshot successfulOfferCreatedEvent(UUID eventId, UUID offerId) {
        return new BusinessEventSnapshot(
                eventId,
                EventType.OFFER_CREATED,
                EventOutcome.SUCCESS,
                EntityType.OFFER,
                offerId.toString(),
                "admin",
                Instant.now(),
                Instant.now(),
                Map.of("merchantName", "Stage 8 Merchant")
        );
    }

    private OfferSnapshot offer(UUID offerId, List<UUID> targetCardProductIds) {
        return new OfferSnapshot(
                offerId,
                "Stage 8 Merchant",
                "Stage 8 offer summary",
                "Online",
                "https://example.com",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600),
                OfferType.ONLINE,
                targetCardProductIds.isEmpty() ? OfferEligibilityMode.ALL : OfferEligibilityMode.CARD_IDS,
                targetCardProductIds,
                null,
                List.of(),
                null,
                List.of(),
                null,
                null,
                List.of(),
                null
        );
    }

    private String requestJson(String priority,
                               String sendMode,
                               String scheduledFor,
                               String channels,
                               String title,
                               String body) {
        return """
                {
                  "priority": "%s",
                  "sendMode": "%s",
                  "scheduledFor": %s,
                  "channels": %s,
                  "title": %s,
                  "body": %s
                }
                """.formatted(
                priority,
                sendMode,
                scheduledFor == null ? "null" : "\"" + scheduledFor + "\"",
                channels,
                title == null ? "null" : "\"" + title + "\"",
                body == null ? "null" : "\"" + body + "\""
        );
    }

    private static String accessToken(String role, UUID subject) {
        String header = base64Url("""
                {"alg":"RS256","typ":"JWT","kid":"local-dev"}
                """);
        String payload = base64Url("""
                {
                  "iss": "offerwall-user-service",
                  "aud": ["offerwall-api"],
                  "sub": "%s",
                  "exp": %d,
                  "roles": ["%s"]
                }
                """.formatted(subject, Instant.now().plusSeconds(3600).getEpochSecond(), role));
        return header + "." + payload + ".signature";
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
