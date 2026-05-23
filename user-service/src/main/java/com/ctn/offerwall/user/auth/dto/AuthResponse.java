package com.ctn.offerwall.user.auth.dto;

import com.ctn.offerwall.user.auth.AuthResult;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        UserProfileResponse user
) {

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.tokens().accessToken(),
                result.tokens().accessTokenExpiresAt(),
                UserProfileResponse.from(result.user())
        );
    }
}
