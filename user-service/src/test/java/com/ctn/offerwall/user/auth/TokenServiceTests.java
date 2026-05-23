package com.ctn.offerwall.user.auth;

import com.ctn.offerwall.user.config.AuthProperties;
import com.ctn.offerwall.user.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTests {

    private final AuthProperties authProperties = new AuthProperties();
    private final TokenService tokenService = new TokenService(authProperties, new JwtKeyProvider(authProperties));

    @Test
    void issuesJwtAccessTokenAndOpaqueRefreshToken() {
        Instant issuedAt = Instant.parse("2026-05-23T00:00:00Z");
        UUID userId = UUID.randomUUID();

        TokenPair pair = tokenService.issueTokens(
                userId,
                "user@example.com",
                Set.of(UserRole.USER),
                issuedAt,
                Duration.ofMinutes(15),
                Duration.ofDays(30)
        );

        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.accessToken()).contains(".");
        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(pair.accessTokenHash()).isEqualTo(tokenService.hashToken(pair.accessToken()));
        assertThat(pair.refreshTokenHash()).isEqualTo(tokenService.hashToken(pair.refreshToken()));
        assertThat(pair.accessTokenHash()).isNotEqualTo(pair.accessToken());
        assertThat(pair.accessTokenExpiresAt()).isEqualTo(issuedAt.plus(Duration.ofMinutes(15)));
        assertThat(pair.refreshTokenExpiresAt()).isEqualTo(issuedAt.plus(Duration.ofDays(30)));

        JwtAccessTokenClaims claims = tokenService.validateAccessToken(pair.accessToken(), issuedAt);
        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.roles()).containsExactly(UserRole.USER);
    }
}
