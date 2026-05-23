package com.ctn.offerwall.user.auth.dto;

import com.ctn.offerwall.user.domain.NotificationPreferences;

public record NotificationPreferenceResponse(
        boolean emailEnabled,
        boolean inAppEnabled
) {

    public static NotificationPreferenceResponse from(NotificationPreferences preferences) {
        return new NotificationPreferenceResponse(preferences.isEmailEnabled(), preferences.isInAppEnabled());
    }
}
