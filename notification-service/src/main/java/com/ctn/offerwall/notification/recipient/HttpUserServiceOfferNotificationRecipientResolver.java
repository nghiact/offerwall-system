package com.ctn.offerwall.notification.recipient;

import com.ctn.offerwall.notification.config.NotificationClientProperties;
import com.ctn.offerwall.notification.config.NotificationSecurityProperties;
import com.ctn.offerwall.notification.exception.UserInputException;
import com.ctn.offerwall.notification.security.NotificationSecurityService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class HttpUserServiceOfferNotificationRecipientResolver implements OfferNotificationRecipientResolver {

    private static final ParameterizedTypeReference<List<OfferNotificationRecipientCandidate>> RECIPIENT_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final NotificationSecurityProperties securityProperties;

    public HttpUserServiceOfferNotificationRecipientResolver(RestClient.Builder restClientBuilder,
                                                             NotificationClientProperties clientProperties,
                                                             NotificationSecurityProperties securityProperties) {
        this.restClient = restClientBuilder
                .clone()
                .baseUrl(clientProperties.getUser().getBaseUrl())
                .build();
        this.securityProperties = securityProperties;
    }

    @Override
    public List<OfferNotificationRecipientCandidate> resolveRecipients(OfferNotificationRecipientQuery query) {
        try {
            List<OfferNotificationRecipientCandidate> recipients = restClient.post()
                    .uri("/internal/notification-recipients/offers")
                    .header(NotificationSecurityService.INTERNAL_SERVICE_KEY_HEADER, internalServiceKey())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(query)
                    .retrieve()
                    .body(RECIPIENT_LIST);
            return recipients == null ? List.of() : recipients;
        } catch (RestClientException exception) {
            throw new UserInputException("Offer notification recipients could not be resolved.");
        }
    }

    private String internalServiceKey() {
        String key = securityProperties.getInternal().getApiKey();
        return key == null ? "" : key;
    }
}
