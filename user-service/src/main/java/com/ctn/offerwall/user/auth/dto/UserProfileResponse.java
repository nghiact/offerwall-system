package com.ctn.offerwall.user.auth.dto;

import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.domain.UserRole;

import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        Set<UserRole> roles,
        NotificationPreferenceResponse notificationPreferences
) {

    public static UserProfileResponse from(AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                Set.copyOf(user.getRoles()),
                NotificationPreferenceResponse.from(user.getNotificationPreferences())
        );
    }
}
