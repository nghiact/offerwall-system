package com.ctn.offerwall.card.security;

import com.ctn.offerwall.card.config.CardSecurityProperties;
import com.ctn.offerwall.card.exception.AuthenticationRequiredException;
import com.ctn.offerwall.card.exception.AuthorizationDeniedException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CardSecurityService {

    private static final String INVALID_TOKEN = "Bearer access token is invalid or expired.";
    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "EDITOR");

    private final CardSecurityProperties properties;
    private final RSAPublicKey publicKey;

    public CardSecurityService(CardSecurityProperties properties) {
        this.properties = properties;
        this.publicKey = hasText(properties.getPublicKeyPem()) ? parsePublicKey(properties.getPublicKeyPem()) : null;
        if (properties.isRequireSignature() && publicKey == null) {
            throw new IllegalStateException("JWT public key PEM is required when signature validation is enabled.");
        }
    }

    public void requireAuthenticated(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (publicKey != null) {
            parseClaims(token);
        }
    }

    public void requireEditorOrAdmin(String authorizationHeader) {
        JWTClaimsSet claims = parseClaims(extractBearerToken(authorizationHeader));
        if (roles(claims).stream().noneMatch(WRITE_ROLES::contains)) {
            throw new AuthorizationDeniedException("Admin or editor role is required.");
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AuthenticationRequiredException("Bearer access token is required.");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new AuthenticationRequiredException("Bearer access token is required.");
        }
        return token;
    }

    private JWTClaimsSet parseClaims(String token) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (publicKey != null && !signedJwt.verify(new RSASSAVerifier(publicKey))) {
                throw invalidToken();
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            if (!properties.getIssuer().equals(claims.getIssuer())) {
                throw invalidToken();
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(properties.getAudience())) {
                throw invalidToken();
            }
            if (claims.getExpirationTime() == null || !claims.getExpirationTime().toInstant().isAfter(Instant.now())) {
                throw invalidToken();
            }
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                throw invalidToken();
            }
            return claims;
        } catch (ParseException | JOSEException | IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private Set<String> roles(JWTClaimsSet claims) {
        try {
            List<String> roles = claims.getStringListClaim("roles");
            if (roles == null) {
                return Set.of();
            }
            return roles.stream()
                    .map(String::trim)
                    .filter(role -> !role.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (ParseException exception) {
            throw invalidToken();
        }
    }

    private AuthenticationRequiredException invalidToken() {
        return new AuthenticationRequiredException(INVALID_TOKEN);
    }

    private RSAPublicKey parsePublicKey(String publicKeyPem) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] keyBytes = decodePem(publicKeyPem);
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("JWT public key must be an RSA X.509 PEM key.", exception);
        }
    }

    private byte[] decodePem(String pem) {
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
