package com.ctn.offerwall.notification.recipient;

import com.ctn.offerwall.notification.notification.dto.NotificationRecipientRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OfferNotificationRecipientCandidateTests {

    @Test
    void mapsToNotificationRecipientRequest() {
        UUID userId = UUID.randomUUID();
        OfferNotificationRecipientCandidate candidate = new OfferNotificationRecipientCandidate(
                userId,
                "user@example.com",
                true,
                false
        );

        NotificationRecipientRequest request = candidate.toNotificationRecipientRequest();

        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.email()).isEqualTo("user@example.com");
        assertThat(request.emailEnabled()).isTrue();
        assertThat(request.inAppEnabled()).isFalse();
    }
}
