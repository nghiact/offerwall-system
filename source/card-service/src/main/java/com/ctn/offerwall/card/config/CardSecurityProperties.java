package com.ctn.offerwall.card.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "offerwall.security.jwt")
public class CardSecurityProperties {

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
