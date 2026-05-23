package com.ctn.offerwall.user.auth;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signUpReturnsAccessTokenRefreshCookieAndCurrentUser() throws Exception {
        String email = "stage3-" + UUID.randomUUID() + "@example.com";

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
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.notificationPreferences.emailEnabled").value(false))
                .andExpect(jsonPath("$.user.notificationPreferences.inAppEnabled").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String accessToken = json.get("accessToken").asText();
        assertThat(accessToken).contains(".");

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(patch("/users/me/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailEnabled": true,
                                  "inAppEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationPreferences.emailEnabled").value(true))
                .andExpect(jsonPath("$.notificationPreferences.inAppEnabled").value(false));
    }

    @Test
    void loginFailureUsesGenericMessage() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing-%s@example.com",
                                  "password": "wrong-password"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email address and/or password is incorrect."));
    }
}
