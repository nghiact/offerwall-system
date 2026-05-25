package com.ctn.offerwall.notification.notification.dto;

import com.ctn.offerwall.common.notification.NotificationChannel;
import com.ctn.offerwall.common.notification.NotificationPriority;
import com.ctn.offerwall.notification.domain.NotificationSendMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record NotificationRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 2000)
        String body,

        @NotNull
        NotificationPriority priority,

        @NotNull
        NotificationSendMode sendMode,

        @FutureOrPresent
        Instant scheduledFor,

        UUID offerId,

        UUID sourceEventId,

        @NotEmpty
        Set<NotificationChannel> channels,

        @NotEmpty
        List<@Valid NotificationRecipientRequest> recipients
) {
}
