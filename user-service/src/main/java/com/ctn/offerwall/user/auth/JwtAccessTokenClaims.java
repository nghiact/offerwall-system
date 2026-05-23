package com.ctn.offerwall.user.auth;

import com.ctn.offerwall.user.domain.UserRole;

import java.util.Set;
import java.util.UUID;

public record JwtAccessTokenClaims(
        UUID userId,
        String email,
        Set<UserRole> roles
) {
}
