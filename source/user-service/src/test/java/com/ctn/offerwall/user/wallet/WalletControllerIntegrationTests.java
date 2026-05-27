package com.ctn.offerwall.user.wallet;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.user.card.CardProductClient;
import com.ctn.offerwall.user.card.dto.CardProductSummaryResponse;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "offerwall.tracking.enabled=false",
        "offerwall.security.internal.api-key=wallet-internal"
})
@AutoConfigureMockMvc
@Transactional
class WalletControllerIntegrationTests {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
    private static final String INTERNAL_KEY = "wallet-internal";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BusinessEventPublisher eventPublisher;

    @MockitoBean
    private CardProductClient cardProductClient;

    @Test
    void addsListsAndDeletesWalletCards() throws Exception {
        AuthorizationContext auth = signUpAuthorization();
        reset(eventPublisher, cardProductClient);
        UUID cardProductId = UUID.randomUUID();
        when(cardProductClient.lookupProducts(anyList())).thenReturn(List.of(cardProduct(cardProductId)));

        String firstResponse = addWalletCard(auth.authorization(), cardProductId);
        String secondResponse = addWalletCard(auth.authorization(), cardProductId);

        JsonNode first = objectMapper.readTree(firstResponse);
        JsonNode second = objectMapper.readTree(secondResponse);
        assertThat(first.get("id").asText()).isNotEqualTo(second.get("id").asText());
        assertThat(first.get("cardProductId").asText()).isEqualTo(cardProductId.toString());
        assertThat(first.get("cardProduct").get("displayName").asText()).isEqualTo("Testbank Prime Visa Platinum credit card");

        mockMvc.perform(get("/users/me/wallet/cards")
                        .header(HttpHeaders.AUTHORIZATION, auth.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cardProduct.displayName").value("Testbank Prime Visa Platinum credit card"));

        mockMvc.perform(get("/internal/users/{userId}/wallet/candidate", auth.userId())
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(auth.userId().toString()))
                .andExpect(jsonPath("$.cardProductIds[0]").value(cardProductId.toString()));

        mockMvc.perform(delete("/users/me/wallet/cards/{walletCardId}", first.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, auth.authorization()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/me/wallet/cards")
                        .header(HttpHeaders.AUTHORIZATION, auth.authorization()))
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

        AuthorizationContext auth = signUpAuthorization();

        mockMvc.perform(post("/users/me/wallet/cards")
                        .header(HttpHeaders.AUTHORIZATION, auth.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardProductId": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        when(cardProductClient.lookupProducts(anyList())).thenReturn(List.of());
        mockMvc.perform(post("/users/me/wallet/cards")
                        .header(HttpHeaders.AUTHORIZATION, auth.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardProductId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Card product was not found."));

        mockMvc.perform(delete("/users/me/wallet/cards/{walletCardId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth.authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet card was not found."));

        mockMvc.perform(get("/internal/users/{userId}/wallet/candidate", auth.userId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Internal service key is required."));
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
                .andExpect(jsonPath("$.cardProduct.id").value(cardProductId.toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private AuthorizationContext signUpAuthorization() throws Exception {
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
        JsonNode auth = objectMapper.readTree(response);
        return new AuthorizationContext(
                "Bearer " + auth.get("accessToken").asText(),
                UUID.fromString(auth.get("user").get("id").asText())
        );
    }

    private CardProductSummaryResponse cardProduct(UUID cardProductId) {
        return new CardProductSummaryResponse(
                cardProductId,
                "testbank-prime",
                "Testbank",
                "Prime",
                CardNetwork.VISA,
                2,
                "Platinum",
                null,
                CardType.CREDIT,
                true,
                "Testbank Prime Visa Platinum credit card"
        );
    }

    private boolean isWalletEvent(BusinessEvent event, EventType eventType, String walletCardId, UUID cardProductId) {
        return event.eventType() == eventType
                && event.outcome() == EventOutcome.SUCCESS
                && event.entityType() == EntityType.WALLET_CARD
                && walletCardId.equals(event.entityId())
                && cardProductId.toString().equals(event.metadata().get("cardProductId"));
    }

    private record AuthorizationContext(String authorization, UUID userId) {
    }
}
