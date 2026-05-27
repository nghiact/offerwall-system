package com.ctn.offerwall.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "offerwall.security")
public class NotificationSecurityProperties {

    private Jwt jwt = new Jwt();
    private Internal internal = new Internal();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Internal getInternal() {
        return internal;
    }

    public void setInternal(Internal internal) {
        this.internal = internal;
    }

    public static class Jwt {
        private String issuer = "offerwall-user-service";
        private String audience = "offerwall-api";
        private boolean requireSignature;
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

        public boolean isRequireSignature() {
            return requireSignature;
        }

        public void setRequireSignature(boolean requireSignature) {
            this.requireSignature = requireSignature;
        }

        public String getPublicKeyPem() {
            return publicKeyPem;
        }

        public void setPublicKeyPem(String publicKeyPem) {
            this.publicKeyPem = publicKeyPem;
        }
    }

    public static class Internal {
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
