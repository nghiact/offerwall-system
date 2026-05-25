package com.ctn.offerwall.notification.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "offerwall.security.internal.api-key=stage-7-5-internal")
@AutoConfigureMockMvc
@Transactional
class NotificationControllerIntegrationTests {

    private static final String INTERNAL_SERVICE_KEY = "stage-7-5-internal";
    private static final String INTERNAL_SERVICE_KEY_HEADER = "X-Internal-Service-Key";
    private static final String ADMIN_AUTHORIZATION = "Bearer " + accessToken("ADMIN", UUID.randomUUID());
    private static final String USER_AUTHORIZATION = "Bearer " + accessToken("USER", UUID.randomUUID());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsImmediateNotificationAndReadsInAppDelivery() throws Exception {
        UUID userId = UUID.randomUUID();

        String response = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "Offer available",
                                "A matching offer is now active.",
                                "NORMAL",
                                "IMMEDIATE",
                                null,
                                "[\"EMAIL\", \"IN_APP\"]",
                                enabledRecipient(userId, "User@Test.Example"),
                                disabledRecipient(userId, "duplicate@example.com")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Offer available"))
                .andExpect(jsonPath("$.priority").value("NORMAL"))
                .andExpect(jsonPath("$.sendMode").value("IMMEDIATE"))
                .andExpect(jsonPath("$.recipientCount").value(1))
                .andExpect(jsonPath("$.deliveryCount").value(2))
                .andExpect(jsonPath("$.deliveries", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode campaign = objectMapper.readTree(response);
        JsonNode email = deliveryByChannel(campaign, "EMAIL");
        JsonNode inApp = deliveryByChannel(campaign, "IN_APP");
        assertThat(email.get("status").asText()).isEqualTo("SENT");
        assertThat(email.get("recipientEmail").asText()).isEqualTo("user@test.example");
        assertThat(email.get("sentAt").isNull()).isFalse();
        assertThat(inApp.get("status").asText()).isEqualTo("SENT");
        assertThat(inApp.get("sentAt").isNull()).isFalse();

        String inAppResponse = mockMvc.perform(get("/api/notifications/users/{userId}/in-app", userId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthorization(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Offer available"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode inAppList = objectMapper.readTree(inAppResponse);
        String deliveryId = inAppList.get(0).get("deliveryId").asText();

        mockMvc.perform(patch("/api/notifications/in-app/{deliveryId}/read", deliveryId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthorization(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(deliveryId))
                .andExpect(jsonPath("$.readAt").exists());
    }

    @Test
    void appliesRecipientPreferencesAndHighPriorityBypass() throws Exception {
        UUID normalUserId = UUID.randomUUID();
        UUID highUserId = UUID.randomUUID();

        String normalResponse = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "Normal notification",
                                "Normal body",
                                "NORMAL",
                                "IMMEDIATE",
                                null,
                                "[\"EMAIL\", \"IN_APP\"]",
                                disabledRecipient(normalUserId, "normal@example.com")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode normal = objectMapper.readTree(normalResponse);
        assertThat(deliveryByChannel(normal, "EMAIL").get("status").asText()).isEqualTo("SKIPPED");
        assertThat(deliveryByChannel(normal, "IN_APP").get("status").asText()).isEqualTo("SKIPPED");

        mockMvc.perform(get("/api/notifications/users/{userId}/in-app", normalUserId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthorization(normalUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        String highResponse = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "High priority",
                                "High priority body",
                                "HIGH",
                                "IMMEDIATE",
                                null,
                                "[\"EMAIL\", \"IN_APP\"]",
                                disabledRecipient(highUserId, "high@example.com")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode high = objectMapper.readTree(highResponse);
        assertThat(deliveryByChannel(high, "EMAIL").get("status").asText()).isEqualTo("SENT");
        assertThat(deliveryByChannel(high, "EMAIL").get("preferenceBypassed").asBoolean()).isTrue();
        assertThat(deliveryByChannel(high, "IN_APP").get("status").asText()).isEqualTo("SENT");
        assertThat(deliveryByChannel(high, "IN_APP").get("preferenceBypassed").asBoolean()).isTrue();
    }

    @Test
    void supportsQueuedNotificationsAndFailedEmailDeliveries() throws Exception {
        UUID queuedUserId = UUID.randomUUID();
        UUID noEmailUserId = UUID.randomUUID();
        String scheduledFor = Instant.now().plusSeconds(3600).toString();

        String queuedResponse = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "Queued notification",
                                "Queued body",
                                "NORMAL",
                                "QUEUED",
                                scheduledFor,
                                "[\"IN_APP\"]",
                                enabledRecipient(queuedUserId, "queued@example.com")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sendMode").value("QUEUED"))
                .andExpect(jsonPath("$.scheduledFor").value(scheduledFor))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode queued = objectMapper.readTree(queuedResponse);
        assertThat(deliveryByChannel(queued, "IN_APP").get("status").asText()).isEqualTo("QUEUED");
        assertThat(deliveryByChannel(queued, "IN_APP").get("sentAt").isNull()).isTrue();

        String failedEmailResponse = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "Email missing",
                                "Email missing body",
                                "NORMAL",
                                "IMMEDIATE",
                                null,
                                "[\"EMAIL\"]",
                                recipient(noEmailUserId, null, true, true)
                        )))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode failedEmail = objectMapper.readTree(failedEmailResponse);
        JsonNode delivery = deliveryByChannel(failedEmail, "EMAIL");
        assertThat(delivery.get("status").asText()).isEqualTo("FAILED");
        assertThat(delivery.get("failureReason").asText()).isEqualTo("Recipient email is required for email notification.");
    }

    @Test
    void rejectsImmediateScheduledNotificationsAndNonInAppRead() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "Invalid scheduled immediate",
                                "Invalid body",
                                "NORMAL",
                                "IMMEDIATE",
                                Instant.now().plusSeconds(3600).toString(),
                                "[\"IN_APP\"]",
                                enabledRecipient(userId, "user@example.com")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("IMMEDIATE notifications cannot define scheduledFor."));

        String response = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "Email only",
                                "Email only body",
                                "NORMAL",
                                "IMMEDIATE",
                                null,
                                "[\"EMAIL\"]",
                                enabledRecipient(userId, "user@example.com")
                        )))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode campaign = objectMapper.readTree(response);
        String emailDeliveryId = deliveryByChannel(campaign, "EMAIL").get("id").asText();

        mockMvc.perform(patch("/api/notifications/in-app/{deliveryId}/read", emailDeliveryId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthorization(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only in-app notifications can be marked read."));
    }

    @Test
    void protectsManagementEndpoints() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = notificationJson(
                "Protected notification",
                "Protected body",
                "NORMAL",
                "IMMEDIATE",
                null,
                "[\"IN_APP\"]",
                enabledRecipient(userId, "user@example.com")
        );

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bearer access token is required."));

        mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, USER_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin or editor role is required."));
    }

    @Test
    void allowsInternalServiceKeyForManagementEndpoints() throws Exception {
        UUID userId = UUID.randomUUID();

        String response = mockMvc.perform(post("/api/notifications")
                        .header(INTERNAL_SERVICE_KEY_HEADER, INTERNAL_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "Internal notification",
                                "Internal body",
                                "NORMAL",
                                "IMMEDIATE",
                                null,
                                "[\"IN_APP\"]",
                                enabledRecipient(userId, "user@example.com")
                        )))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode campaign = objectMapper.readTree(response);
        mockMvc.perform(get("/api/notifications/{id}", campaign.get("id").asText())
                        .header(INTERNAL_SERVICE_KEY_HEADER, INTERNAL_SERVICE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(campaign.get("id").asText()));
    }

    @Test
    void usesSourceEventIdAsIdempotencyKey() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();
        String body = notificationJsonWithSourceEventId(
                "Idempotent notification",
                "Idempotent body",
                "NORMAL",
                "IMMEDIATE",
                null,
                sourceEventId,
                "[\"IN_APP\"]",
                enabledRecipient(userId, "user@example.com")
        );

        String firstResponse = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstResponse);
        JsonNode second = objectMapper.readTree(secondResponse);
        assertThat(second.get("id").asText()).isEqualTo(first.get("id").asText());
        assertThat(second.get("deliveryCount").asInt()).isEqualTo(1);
    }

    @Test
    void rejectsInboxAccessForOtherUser() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        String response = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationJson(
                                "Owner notification",
                                "Owner body",
                                "NORMAL",
                                "IMMEDIATE",
                                null,
                                "[\"IN_APP\"]",
                                enabledRecipient(ownerUserId, "owner@example.com")
                        )))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode campaign = objectMapper.readTree(response);
        String deliveryId = deliveryByChannel(campaign, "IN_APP").get("id").asText();

        mockMvc.perform(get("/api/notifications/users/{userId}/in-app", ownerUserId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthorization(otherUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Notification belongs to another user."));

        mockMvc.perform(patch("/api/notifications/in-app/{deliveryId}/read", deliveryId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthorization(otherUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Notification belongs to another user."));
    }

    private JsonNode deliveryByChannel(JsonNode campaign, String channel) {
        for (JsonNode delivery : campaign.get("deliveries")) {
            if (channel.equals(delivery.get("channel").asText())) {
                return delivery;
            }
        }
        throw new AssertionError("Missing delivery for channel " + channel);
    }

    private String notificationJson(String title,
                                    String body,
                                    String priority,
                                    String sendMode,
                                    String scheduledFor,
                                    String channels,
                                    String... recipients) {
        return notificationJsonWithSourceEventId(
                title,
                body,
                priority,
                sendMode,
                scheduledFor,
                UUID.randomUUID(),
                channels,
                recipients
        );
    }

    private String notificationJsonWithSourceEventId(String title,
                                                     String body,
                                                     String priority,
                                                     String sendMode,
                                                     String scheduledFor,
                                                     UUID sourceEventId,
                                                     String channels,
                                                     String... recipients) {
        return """
                {
                  "title": "%s",
                  "body": "%s",
                  "priority": "%s",
                  "sendMode": "%s",
                  "scheduledFor": %s,
                  "offerId": "%s",
                  "sourceEventId": "%s",
                  "channels": %s,
                  "recipients": [%s]
                }
                """.formatted(
                title,
                body,
                priority,
                sendMode,
                scheduledFor == null ? "null" : "\"" + scheduledFor + "\"",
                UUID.randomUUID(),
                sourceEventId,
                channels,
                String.join(",", recipients)
        );
    }

    private String enabledRecipient(UUID userId, String email) {
        return recipient(userId, email, true, true);
    }

    private String disabledRecipient(UUID userId, String email) {
        return recipient(userId, email, false, false);
    }

    private String recipient(UUID userId, String email, boolean emailEnabled, boolean inAppEnabled) {
        return """
                {
                  "userId": "%s",
                  "email": %s,
                  "emailEnabled": %s,
                  "inAppEnabled": %s
                }
                """.formatted(
                userId,
                email == null ? "null" : "\"" + email + "\"",
                emailEnabled,
                inAppEnabled
        );
    }

    private String userAuthorization(UUID userId) {
        return "Bearer " + accessToken("USER", userId);
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
