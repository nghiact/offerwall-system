package com.ctn.offerwall.user.auth;

import com.ctn.offerwall.user.config.AuthProperties;
import com.ctn.offerwall.user.domain.UserRole;
import com.ctn.offerwall.user.exception.AuthenticationRequiredException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TokenService {

    private static final int TOKEN_BYTES = 32;
    private static final String INVALID_ACCESS_TOKEN = "Access token is invalid or expired.";

    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthProperties authProperties;
    private final JwtKeyProvider jwtKeyProvider;

    public TokenService(AuthProperties authProperties, JwtKeyProvider jwtKeyProvider) {
        this.authProperties = authProperties;
        this.jwtKeyProvider = jwtKeyProvider;
    }

    public TokenPair issueTokens(UUID userId,
                                 String email,
                                 Set<UserRole> roles,
                                 Instant issuedAt,
                                 Duration accessTokenTtl,
                                 Duration refreshTokenTtl) {
        Instant accessTokenExpiresAt = issuedAt.plus(accessTokenTtl);
        String accessToken = issueAccessToken(userId, email, roles, issuedAt, accessTokenExpiresAt);
        String refreshToken = generateToken();
        return new TokenPair(
                accessToken,
                hashToken(accessToken),
                accessTokenExpiresAt,
                refreshToken,
                hashToken(refreshToken),
                issuedAt.plus(refreshTokenTtl)
        );
    }

    public JwtAccessTokenClaims validateAccessToken(String token, Instant now) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (!signedJwt.verify(new RSASSAVerifier(jwtKeyProvider.publicKey()))) {
                throw invalidAccessToken();
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            validateClaims(claims, now);
            return toAccessTokenClaims(claims);
        } catch (ParseException | JOSEException | IllegalArgumentException exception) {
            throw invalidAccessToken();
        }
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String issueAccessToken(UUID userId,
                                    String email,
                                    Set<UserRole> roles,
                                    Instant issuedAt,
                                    Instant expiresAt) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(authProperties.getJwt().getIssuer())
                    .audience(authProperties.getJwt().getAudience())
                    .subject(userId.toString())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("email", email)
                    .claim("roles", roles.stream().map(UserRole::name).sorted().toList())
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(authProperties.getJwt().getKeyId())
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT signedJwt = new SignedJWT(header, claims);
            signedJwt.sign(new RSASSASigner(jwtKeyProvider.privateKey()));
            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to issue access token.", exception);
        }
    }

    private void validateClaims(JWTClaimsSet claims, Instant now) {
        if (!authProperties.getJwt().getIssuer().equals(claims.getIssuer())) {
            throw invalidAccessToken();
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(authProperties.getJwt().getAudience())) {
            throw invalidAccessToken();
        }
        if (claims.getExpirationTime() == null || !claims.getExpirationTime().toInstant().isAfter(now)) {
            throw invalidAccessToken();
        }
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw invalidAccessToken();
        }
    }

    private JwtAccessTokenClaims toAccessTokenClaims(JWTClaimsSet claims) throws ParseException {
        List<String> roleNames = claims.getStringListClaim("roles");
        Set<UserRole> roles = roleNames == null ? Set.of() : roleNames.stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toUnmodifiableSet());

        return new JwtAccessTokenClaims(
                UUID.fromString(claims.getSubject()),
                claims.getStringClaim("email"),
                roles
        );
    }

    private AuthenticationRequiredException invalidAccessToken() {
        return new AuthenticationRequiredException(INVALID_ACCESS_TOKEN);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
