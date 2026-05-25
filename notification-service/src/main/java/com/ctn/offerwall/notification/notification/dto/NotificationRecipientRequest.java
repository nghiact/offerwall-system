package com.ctn.offerwall.notification.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NotificationRecipientRequest(
        @NotNull
        UUID userId,

        @Email
        @Size(max = 320)
        String email,

        boolean emailEnabled,

        boolean inAppEnabled
) {
}
