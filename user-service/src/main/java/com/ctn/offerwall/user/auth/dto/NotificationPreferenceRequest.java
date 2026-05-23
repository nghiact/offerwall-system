package com.ctn.offerwall.user.auth.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceRequest(
        @NotNull Boolean emailEnabled,
        @NotNull Boolean inAppEnabled
) {
}
