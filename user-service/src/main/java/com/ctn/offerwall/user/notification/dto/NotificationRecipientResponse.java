package com.ctn.offerwall.user.notification.dto;

import com.ctn.offerwall.user.domain.AppUser;

import java.util.UUID;

public record NotificationRecipientResponse(
        UUID userId,
        String email,
        boolean emailEnabled,
        boolean inAppEnabled
) {

    public static NotificationRecipientResponse from(AppUser user) {
        return new NotificationRecipientResponse(
                user.getId(),
                user.getEmail(),
                user.getNotificationPreferences().isEmailEnabled(),
                user.getNotificationPreferences().isInAppEnabled()
        );
    }
}
