package com.ctn.offerwall.card.card;

import com.ctn.offerwall.card.repository.CardBinRepository;
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

import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "offerwall.security.internal.api-key=stage-10-internal")
@AutoConfigureMockMvc
@Transactional
class CardControllerIntegrationTests {

    private static final String AUTHORIZATION = "Bearer local-test-token";
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
    private static final String INTERNAL_KEY = "stage-10-internal";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardBinRepository binRepository;

    @Test
    void createsCardAndMatchesByPrefix() throws Exception {
        String base = randomBinBase();
        createCard("testbank-prime-" + base, new String[]{base + "01"}, "Testbank", "Prime", "VISA", 2, null, "CREDIT");
        createCard("testbank-prime-plus-" + base, new String[]{base + "0199"}, "Testbank", "Prime Plus", "VISA", 3, null, "CREDIT");
        createCard("otherbank-napas-" + base, new String[]{base + "88"}, "Otherbank", null, "NAPAS", 2, null, "DEBIT");

        mockMvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cards/matches")
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .param("prefix", base.substring(0, 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/api/cards/matches")
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .param("prefix", base + "01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].displayName").value("Testbank Prime Visa Platinum credit card"))
                .andExpect(jsonPath("$[0].matchedBins[0]").value(base + "01"))
                .andExpect(jsonPath("$[1].displayName").value("Testbank Prime Plus Visa Signature credit card"))
                .andExpect(jsonPath("$[1].matchedBins[0]").value(base + "0199"));
    }

    @Test
    void rejectsMissingAuthorization() throws Exception {
        mockMvc.perform(get("/api/cards/matches")
                        .param("prefix", "1234"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bearer access token is required."));
    }

    @Test
    void rejectsInvalidLookupPrefix() throws Exception {
        mockMvc.perform(get("/api/cards/matches")
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .param("prefix", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Card lookup prefix must be 4 to 8 digits."));
    }

    @Test
    void allowsDuplicateBinAcrossProducts() throws Exception {
        String bin = randomBinBase() + "44";
        createCard("testbank-dup-" + bin, new String[]{bin}, "Testbank", null, "MASTERCARD", 1, null, "DEBIT");

        mockMvc.perform(post("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("otherbank-dup-" + bin, new String[]{bin}, "Otherbank", null, "MASTERCARD", 1, null, "DEBIT")))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsDuplicateProductCode() throws Exception {
        String bin = randomBinBase() + "55";
        String productCode = "duplicate-code-" + bin;
        createCard(productCode, new String[]{bin}, "Testbank", null, "VISA", 1, null, "CREDIT");

        mockMvc.perform(post("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(productCode, new String[]{bin + "9"}, "Otherbank", null, "VISA", 1, null, "CREDIT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Card product code already exists."));
    }

    @Test
    void rejectsDuplicateBinInsideSameProduct() throws Exception {
        String bin = randomBinBase() + "66";

        mockMvc.perform(post("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("duplicate-bin-product-" + bin, new String[]{bin, bin}, "Testbank", null, "VISA", 1, null, "CREDIT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Card product cannot contain duplicate BINs."));
    }

    @Test
    void looksUpInternalCardProductSummaries() throws Exception {
        String base = randomBinBase();
        JsonNode created = createCard("lookup-card-" + base, new String[]{base + "77"}, "Lookupbank", "Reserve",
                "VISA", 3, null, "CREDIT");

        mockMvc.perform(post("/internal/card-products/lookup")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardProductIds": ["%s"]
                                }
                                """.formatted(created.get("id").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(created.get("id").asText()))
                .andExpect(jsonPath("$[0].issuer").value("Lookupbank"))
                .andExpect(jsonPath("$[0].network").value("VISA"))
                .andExpect(jsonPath("$[0].tier").value(3))
                .andExpect(jsonPath("$[0].type").value("CREDIT"))
                .andExpect(jsonPath("$[0].displayName").value("Lookupbank Reserve Visa Signature credit card"));

        mockMvc.perform(post("/internal/card-products/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardProductIds": ["%s"]
                                }
                                """.formatted(created.get("id").asText())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Internal service key is required."));
    }

    private JsonNode createCard(String productCode,
                                String[] bins,
                                String issuer,
                                String name,
                                String network,
                                int tier,
                                String tierLabelOverride,
                                String type) throws Exception {
        String response = mockMvc.perform(post("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(productCode, bins, issuer, name, network, tier, tierLabelOverride, type)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String cardJson(String productCode,
                            String[] bins,
                            String issuer,
                            String name,
                            String network,
                            int tier,
                            String tierLabelOverride,
                            String type) {
        return """
                {
                  "productCode": "%s",
                  "bins": [%s],
                  "issuer": "%s",
                  "name": %s,
                  "network": "%s",
                  "tier": %d,
                  "tierLabelOverride": %s,
                  "type": "%s",
                  "personal": true
                }
                """.formatted(
                productCode,
                quotedBins(bins),
                issuer,
                name == null ? "null" : "\"" + name + "\"",
                network,
                tier,
                tierLabelOverride == null ? "null" : "\"" + tierLabelOverride + "\"",
                type
        );
    }

    private String quotedBins(String[] bins) {
        return java.util.Arrays.stream(bins)
                .map(bin -> "\"" + bin + "\"")
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String randomBinBase() {
        String candidate;
        do {
            candidate = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        } while (binRepository.existsByBin(candidate + "01")
                || binRepository.existsByBin(candidate + "44")
                || binRepository.existsByBin(candidate + "55")
                || binRepository.existsByBin(candidate + "66"));
        return candidate;
    }
}
