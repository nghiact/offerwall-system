package com.ctn.offerwall.user.wallet;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.user.tracking.BusinessEventPublisher;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "offerwall.tracking.enabled=false")
@AutoConfigureMockMvc
@Transactional
class WalletControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BusinessEventPublisher eventPublisher;

    @Test
    void addsListsAndDeletesWalletCards() throws Exception {
        String authorization = signUpAuthorization();
        reset(eventPublisher);
        UUID cardProductId = UUID.randomUUID();

        String firstResponse = addWalletCard(authorization, cardProductId);
        String secondResponse = addWalletCard(authorization, cardProductId);

        JsonNode first = objectMapper.readTree(firstResponse);
        JsonNode second = objectMapper.readTree(secondResponse);
        assertThat(first.get("id").asText()).isNotEqualTo(second.get("id").asText());
        assertThat(first.get("cardProductId").asText()).isEqualTo(cardProductId.toString());

        mockMvc.perform(get("/users/me/wallet/cards")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(delete("/users/me/wallet/cards/{walletCardId}", first.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/me/wallet/cards")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(second.get("id").asText()));

        verify(eventPublisher).publish(argThat(event ->
                isWalletEvent(event, EventType.WALLET_CARD_ADDED, first.get("id").asText(), cardProductId)
        ));
        verify(eventPublisher).publish(argThat(event ->
                isWalletEvent(event, EventType.WALLET_CARD_DELETED, first.get("id").asText(), cardProductId)
        ));
    }

    @Test
    void rejectsMissingAuthInvalidRequestAndMissingWalletCard() throws Exception {
        mockMvc.perform(get("/users/me/wallet/cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bearer access token is required."));

        String authorization = signUpAuthorization();

        mockMvc.perform(post("/users/me/wallet/cards")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardProductId": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/users/me/wallet/cards/{walletCardId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet card was not found."));
    }

    private String addWalletCard(String authorization, UUID cardProductId) throws Exception {
        return mockMvc.perform(post("/users/me/wallet/cards")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardProductId": "%s"
                                }
                                """.formatted(cardProductId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cardProductId").value(cardProductId.toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String signUpAuthorization() throws Exception {
        String email = "wallet-" + UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "ValidPass1!",
                                  "confirmPassword": "ValidPass1!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return "Bearer " + objectMapper.readTree(response).get("accessToken").asText();
    }

    private boolean isWalletEvent(BusinessEvent event, EventType eventType, String walletCardId, UUID cardProductId) {
        return event.eventType() == eventType
                && event.outcome() == EventOutcome.SUCCESS
                && event.entityType() == EntityType.WALLET_CARD
                && walletCardId.equals(event.entityId())
                && cardProductId.toString().equals(event.metadata().get("cardProductId"));
    }
}
