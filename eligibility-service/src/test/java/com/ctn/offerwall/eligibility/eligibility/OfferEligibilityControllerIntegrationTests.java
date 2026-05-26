package com.ctn.offerwall.eligibility.eligibility;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.eligibility.card.CardProductClient;
import com.ctn.offerwall.eligibility.card.CardProductSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "offerwall.security.internal.api-key=stage-10-internal")
@AutoConfigureMockMvc
class OfferEligibilityControllerIntegrationTests {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
    private static final String INTERNAL_KEY = "stage-10-internal";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardProductClient cardProductClient;

    @Test
    void resolvesCriteriaAgainstCardProducts() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID visaInfiniteCredit = UUID.randomUUID();
        UUID visaDebit = UUID.randomUUID();
        UUID mastercardCredit = UUID.randomUUID();

        when(cardProductClient.lookupProducts(anyList())).thenReturn(List.of(
                card(visaInfiniteCredit, "Testbank", CardNetwork.VISA, 4, CardType.CREDIT, true),
                card(visaDebit, "Testbank", CardNetwork.VISA, 2, CardType.DEBIT, true),
                card(mastercardCredit, "Otherbank", CardNetwork.MASTERCARD, 2, CardType.CREDIT, true)
        ));

        mockMvc.perform(post("/internal/eligibility/offers/users")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eligibilityMode": "CRITERIA",
                                  "targetCardProductIds": [],
                                  "targetIssuers": ["testbank", "anotherbank"],
                                  "targetNetworks": ["VISA", "JCB"],
                                  "targetTier": 3,
                                  "targetTypes": ["CREDIT", "PREPAID"],
                                  "targetPersonal": true,
                                  "candidates": [
                                    {
                                      "userId": "%s",
                                      "cardProductIds": ["%s", "%s"]
                                    },
                                    {
                                      "userId": "%s",
                                      "cardProductIds": ["%s"]
                                    }
                                  ]
                                }
                                """.formatted(userA, visaInfiniteCredit, mastercardCredit, userB, visaDebit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleUserIds[0]").value(userA.toString()));
    }

    @Test
    void resolvesAllAndCardIdModesWithoutCardLookup() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID cardA = UUID.randomUUID();
        UUID cardB = UUID.randomUUID();

        mockMvc.perform(post("/internal/eligibility/offers/users")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eligibilityMode": "ALL",
                                  "targetCardProductIds": [],
                                  "candidates": [
                                    {"userId": "%s", "cardProductIds": ["%s"]},
                                    {"userId": "%s", "cardProductIds": ["%s"]}
                                  ]
                                }
                                """.formatted(userA, cardA, userB, cardB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleUserIds", containsInAnyOrder(userA.toString(), userB.toString())));

        mockMvc.perform(post("/internal/eligibility/offers/users")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eligibilityMode": "CARD_IDS",
                                  "targetCardProductIds": ["%s"],
                                  "candidates": [
                                    {"userId": "%s", "cardProductIds": ["%s"]},
                                    {"userId": "%s", "cardProductIds": ["%s"]}
                                  ]
                                }
                                """.formatted(cardB, userA, cardA, userB, cardB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleUserIds[0]").value(userB.toString()));

        verify(cardProductClient, never()).lookupProducts(anyList());
    }

    @Test
    void rejectsCriteriaWithoutCriteria() throws Exception {
        mockMvc.perform(post("/internal/eligibility/offers/users")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eligibilityMode": "CRITERIA",
                                  "targetCardProductIds": [],
                                  "targetIssuers": [],
                                  "targetNetworks": [],
                                  "targetTier": null,
                                  "targetTypes": [],
                                  "targetPersonal": null,
                                  "candidates": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CRITERIA eligibility requires at least one criterion."));
    }

    @Test
    void rejectsMissingInternalKey() throws Exception {
        mockMvc.perform(post("/internal/eligibility/offers/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eligibilityMode": "ALL",
                                  "candidates": []
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Internal service key is required."));
    }

    private CardProductSummary card(UUID id, String issuer, CardNetwork network, int tier, CardType type, boolean personal) {
        return new CardProductSummary(
                id,
                "product-" + id,
                issuer,
                null,
                network,
                tier,
                null,
                null,
                type,
                personal,
                issuer + " " + network + " card"
        );
    }
}
