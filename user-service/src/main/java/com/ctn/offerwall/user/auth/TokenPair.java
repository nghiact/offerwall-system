package com.ctn.offerwall.user.auth;

import java.time.Instant;

public record TokenPair(
        String accessToken,
        String accessTokenHash,
        Instant accessTokenExpiresAt,
        String refreshToken,
        String refreshTokenHash,
        Instant refreshTokenExpiresAt
) {
}
