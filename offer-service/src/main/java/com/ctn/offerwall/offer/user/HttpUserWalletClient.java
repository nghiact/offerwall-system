package com.ctn.offerwall.offer.user;

import com.ctn.offerwall.offer.config.UserClientProperties;
import com.ctn.offerwall.offer.exception.UpstreamServiceException;
import com.ctn.offerwall.offer.security.InternalServiceHeaders;
import com.ctn.offerwall.offer.user.dto.UserWalletCandidate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class HttpUserWalletClient implements UserWalletClient {

    private final UserClientProperties properties;
    private final RestClient restClient;

    public HttpUserWalletClient(UserClientProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public UserWalletCandidate getWalletCandidate(UUID userId) {
        try {
            UserWalletCandidate response = restClient.get()
                    .uri("/internal/users/{userId}/wallet/candidate", userId)
                    .header(InternalServiceHeaders.INTERNAL_SERVICE_KEY_HEADER, headerValue(properties.getInternalApiKey()))
                    .retrieve()
                    .body(UserWalletCandidate.class);
            return response == null ? new UserWalletCandidate(userId, null) : response;
        } catch (RestClientException exception) {
            throw new UpstreamServiceException("User wallet lookup failed.", exception);
        }
    }

    private String headerValue(String value) {
        return value == null ? "" : value;
    }
}
