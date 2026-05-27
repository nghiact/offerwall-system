package com.ctn.offerwall.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "offerwall.auth")
public class AuthProperties {

    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(30);
    private Jwt jwt = new Jwt();
    private RefreshCookie refreshCookie = new RefreshCookie();

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public RefreshCookie getRefreshCookie() {
        return refreshCookie;
    }

    public void setRefreshCookie(RefreshCookie refreshCookie) {
        this.refreshCookie = refreshCookie;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public static class Jwt {

        private String issuer = "offerwall-user-service";
        private String audience = "offerwall-api";
        private String keyId = "local-dev";
        private String privateKeyPem;
        private String publicKeyPem;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getPrivateKeyPem() {
            return privateKeyPem;
        }

        public void setPrivateKeyPem(String privateKeyPem) {
            this.privateKeyPem = privateKeyPem;
        }

        public String getPublicKeyPem() {
            return publicKeyPem;
        }

        public void setPublicKeyPem(String publicKeyPem) {
            this.publicKeyPem = publicKeyPem;
        }
    }

    public static class RefreshCookie {

        private String name = "refresh_token";
        private boolean secure;
        private String sameSite = "Lax";
        private String path = "/";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
