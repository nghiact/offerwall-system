package com.ctn.offerwall.notification.notification.dto;

import com.ctn.offerwall.common.notification.NotificationChannel;
import com.ctn.offerwall.common.notification.NotificationPriority;
import com.ctn.offerwall.notification.domain.NotificationSendMode;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record OfferCreatedNotificationRequest(
        @NotNull
        NotificationPriority priority,

        @NotNull
        NotificationSendMode sendMode,

        @FutureOrPresent
        Instant scheduledFor,

        @NotEmpty
        Set<NotificationChannel> channels,

        @Size(max = 200)
        String title,

        @Size(max = 2000)
        String body
) {
}
