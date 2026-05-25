package com.ctn.offerwall.user.notification;

import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.domain.NotificationPreferences;
import com.ctn.offerwall.user.domain.WalletCard;
import com.ctn.offerwall.user.repository.AppUserRepository;
import com.ctn.offerwall.user.repository.WalletCardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "offerwall.security.internal.api-key=stage-8-internal",
        "offerwall.tracking.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class NotificationRecipientControllerIntegrationTests {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
    private static final String INTERNAL_KEY = "stage-8-internal";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private WalletCardRepository walletCardRepository;

    @Test
    void resolvesCardIdOfferRecipientsFromWalletCards() throws Exception {
        UUID matchingCardProductId = UUID.randomUUID();
        AppUser matchingUser = saveUser("wallet-match", true, false);
        AppUser otherUser = saveUser("wallet-other", true, true);
        walletCardRepository.save(new WalletCard(matchingUser, matchingCardProductId.toString()));
        walletCardRepository.save(new WalletCard(otherUser, UUID.randomUUID().toString()));

        String response = mockMvc.perform(post("/internal/notification-recipients/offers")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("CARD_IDS", "[\"" + matchingCardProductId + "\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode recipients = objectMapper.readTree(response);
        assertThat(recipients.get(0).get("userId").asText()).isEqualTo(matchingUser.getId().toString());
        assertThat(recipients.get(0).get("email").asText()).isEqualTo(matchingUser.getEmail());
        assertThat(recipients.get(0).get("emailEnabled").asBoolean()).isTrue();
        assertThat(recipients.get(0).get("inAppEnabled").asBoolean()).isFalse();
    }

    @Test
    void resolvesAllOfferRecipientsFromAllUsers() throws Exception {
        AppUser firstUser = saveUser("all-first", false, true);
        AppUser secondUser = saveUser("all-second", true, true);

        String response = mockMvc.perform(post("/internal/notification-recipients/offers")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ALL", "[]")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode recipients = objectMapper.readTree(response);
        assertThat(containsUser(recipients, firstUser.getId())).isTrue();
        assertThat(containsUser(recipients, secondUser.getId())).isTrue();
    }

    @Test
    void rejectsMissingInternalKeyAndUnsupportedCriteriaResolution() throws Exception {
        mockMvc.perform(post("/internal/notification-recipients/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ALL", "[]")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Internal service key is required."));

        mockMvc.perform(post("/internal/notification-recipients/offers")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("CRITERIA", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CRITERIA recipient resolution is not implemented yet."));
    }

    private AppUser saveUser(String prefix, boolean emailEnabled, boolean inAppEnabled) {
        return userRepository.save(new AppUser(
                prefix + "-" + UUID.randomUUID() + "@example.com",
                "ValidPass1!",
                new NotificationPreferences(emailEnabled, inAppEnabled)
        ));
    }

    private boolean containsUser(JsonNode recipients, UUID userId) {
        for (JsonNode recipient : recipients) {
            if (userId.toString().equals(recipient.get("userId").asText())) {
                return true;
            }
        }
        return false;
    }

    private String requestJson(String eligibilityMode, String targetCardProductIds) {
        return """
                {
                  "offerId": "%s",
                  "sourceEventId": "%s",
                  "eligibilityMode": "%s",
                  "targetCardProductIds": %s,
                  "targetIssuer": null,
                  "targetNetwork": null,
                  "targetTier": null,
                  "targetType": null,
                  "targetPersonal": null
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), eligibilityMode, targetCardProductIds);
    }
}
