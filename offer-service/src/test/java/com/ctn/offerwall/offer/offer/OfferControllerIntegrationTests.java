package com.ctn.offerwall.offer.offer;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.offer.tracking.BusinessEventPublisher;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OfferControllerIntegrationTests {

    private static final String ADMIN_USER_ID = UUID.randomUUID().toString();
    private static final String EDITOR_USER_ID = UUID.randomUUID().toString();
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String ADMIN_AUTHORIZATION = "Bearer " + accessToken("ADMIN", ADMIN_USER_ID);
    private static final String EDITOR_AUTHORIZATION = "Bearer " + accessToken("EDITOR", EDITOR_USER_ID);
    private static final String USER_AUTHORIZATION = "Bearer " + accessToken("USER", USER_ID);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BusinessEventPublisher eventPublisher;

    @Test
    void createsOfferAndFiltersList() throws Exception {
        String suffix = UUID.randomUUID().toString();
        JsonNode category = createCategory("dining-" + suffix, "Dining " + suffix);
        String merchantName = "Stage5 Merchant " + suffix;

        String offerResponse = mockMvc.perform(post("/api/offers")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerJson(category.get("id").asText(), merchantName, "BOTH", "ALL", null, null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantName").value(merchantName))
                .andExpect(jsonPath("$.category.id").value(category.get("id").asText()))
                .andExpect(jsonPath("$.offerType").value("BOTH"))
                .andExpect(jsonPath("$.eligibilityMode").value("ALL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode offer = objectMapper.readTree(offerResponse);
        verify(eventPublisher, atLeastOnce()).publish(any(BusinessEvent.class));
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.argThat(event ->
                event.eventType() == EventType.OFFER_CREATED
                        && event.outcome() == EventOutcome.SUCCESS
                        && event.entityType() == EntityType.OFFER
                        && offer.get("id").asText().equals(event.entityId())
                        && ADMIN_USER_ID.equals(event.actorUserId())
                        && category.get("id").asText().equals(event.metadata().get("categoryId"))
                        && merchantName.equals(event.metadata().get("merchantName"))
        ));

        mockMvc.perform(get("/api/offers")
                        .param("keyword", suffix)
                        .param("categoryId", category.get("id").asText())
                        .param("type", "BOTH")
                        .param("eligibilityMode", "ALL")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].merchantName").value(merchantName));
    }

    @Test
    void createsCardIdAndCriteriaOffers() throws Exception {
        String suffix = UUID.randomUUID().toString();
        JsonNode category = createCategory("cards-" + suffix, "Cards " + suffix);
        UUID cardProductId = UUID.randomUUID();

        mockMvc.perform(post("/api/offers")
                        .header(HttpHeaders.AUTHORIZATION, EDITOR_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerJson(category.get("id").asText(), "Card Offer " + suffix, "ONLINE", "CARD_IDS", "[\"" + cardProductId + "\"]", null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eligibilityMode").value("CARD_IDS"))
                .andExpect(jsonPath("$.targetCardProductIds[0]").value(cardProductId.toString()));

        mockMvc.perform(post("/api/offers")
                        .header(HttpHeaders.AUTHORIZATION, EDITOR_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerJson(category.get("id").asText(), "Criteria Offer " + suffix, "OFFLINE", "CRITERIA", "[]", "\"Testbank\"", "\"VISA\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eligibilityMode").value("CRITERIA"))
                .andExpect(jsonPath("$.targetIssuer").value("Testbank"))
                .andExpect(jsonPath("$.targetIssuers[0]").value("Testbank"))
                .andExpect(jsonPath("$.targetNetwork").value("VISA"))
                .andExpect(jsonPath("$.targetNetworks[0]").value("VISA"));
    }

    @Test
    void rejectsInvalidOfferRules() throws Exception {
        String suffix = UUID.randomUUID().toString();
        JsonNode category = createCategory("invalid-" + suffix, "Invalid " + suffix);

        mockMvc.perform(post("/api/offers")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerJson(category.get("id").asText(), "Bad Time " + suffix, "ONLINE", "ALL", null, null, null, 3600, 60)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Offer endTime must be after startTime."));

        mockMvc.perform(post("/api/offers")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerJson(category.get("id").asText(), "No Cards " + suffix, "ONLINE", "CARD_IDS", "[]", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CARD_IDS eligibility requires at least one card product ID."));

        mockMvc.perform(post("/api/offers")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerJson(category.get("id").asText(), "Criteria All " + suffix, "ONLINE", "CRITERIA", "[]", null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetIssuers", hasSize(0)))
                .andExpect(jsonPath("$.targetNetworks", hasSize(0)))
                .andExpect(jsonPath("$.targetTypes", hasSize(0)));
    }

    @Test
    void managesCategoriesAndPreventsDeletingUsedCategory() throws Exception {
        String suffix = UUID.randomUUID().toString();
        JsonNode category = createCategory("shopping-" + suffix, "Shopping " + suffix);

        mockMvc.perform(post("/api/offer-categories")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("shopping-" + suffix, "Shopping Duplicate " + suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Offer category code already exists."));

        mockMvc.perform(put("/api/offer-categories/{id}", category.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, EDITOR_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("shopping-updated-" + suffix, "Shopping Updated " + suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("shopping-updated-" + suffix));

        mockMvc.perform(post("/api/offers")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerJson(category.get("id").asText(), "Used Category Merchant " + suffix, "ONLINE", "ALL", null, null, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/offer-categories/{id}", category.get("id").asText()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bearer access token is required."));

        mockMvc.perform(delete("/api/offer-categories/{id}", category.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, USER_AUTHORIZATION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin or editor role is required."));

        mockMvc.perform(delete("/api/offer-categories/{id}", category.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Offer category is in use."));
    }

    private JsonNode createCategory(String code, String name) throws Exception {
        String response = mockMvc.perform(post("/api/offer-categories")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson(code, name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String categoryJson(String code, String name) {
        return """
                {
                  "code": "%s",
                  "name": "%s",
                  "description": "Test category"
                }
                """.formatted(code, name);
    }

    private String offerJson(String categoryId,
                             String merchantName,
                             String offerType,
                             String eligibilityMode,
                             String targetCardProductIds,
                             String targetIssuer,
                             String targetNetwork) {
        return offerJson(categoryId, merchantName, offerType, eligibilityMode, targetCardProductIds, targetIssuer, targetNetwork, -60, 3600);
    }

    private String offerJson(String categoryId,
                             String merchantName,
                             String offerType,
                             String eligibilityMode,
                             String targetCardProductIds,
                             String targetIssuer,
                             String targetNetwork,
                             long startOffsetSeconds,
                             long endOffsetSeconds) {
        Instant now = Instant.now();
        return """
                {
                  "categoryId": "%s",
                  "merchantName": "%s",
                  "offerSummary": "20%% off selected products",
                  "addressDisplay": "Online store",
                  "addressUrl": "https://example.com",
                  "startTime": "%s",
                  "endTime": "%s",
                  "offerType": "%s",
                  "eligibilityMode": "%s",
                  "targetCardProductIds": %s,
                  "targetIssuer": null,
                  "targetIssuers": %s,
                  "targetNetwork": null,
                  "targetNetworks": %s,
                  "targetTier": null,
                  "targetType": null,
                  "targetTypes": [],
                  "targetPersonal": null
                }
                """.formatted(
                categoryId,
                merchantName,
                now.plusSeconds(startOffsetSeconds),
                now.plusSeconds(endOffsetSeconds),
                offerType,
                eligibilityMode,
                targetCardProductIds == null ? "[]" : targetCardProductIds,
                targetIssuer == null ? "[]" : "[" + targetIssuer + "]",
                targetNetwork == null ? "[]" : "[" + targetNetwork + "]"
        );
    }

    private static String accessToken(String role, String subject) {
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
