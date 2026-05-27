package com.ctn.offerwall.offer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "offerwall.clients.user")
public class UserClientProperties {

    private String baseUrl = "http://localhost:8081";
    private String internalApiKey;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getInternalApiKey() {
        return internalApiKey;
    }

    public void setInternalApiKey(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }
}
