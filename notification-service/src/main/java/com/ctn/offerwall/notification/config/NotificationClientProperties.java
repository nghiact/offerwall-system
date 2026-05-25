package com.ctn.offerwall.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "offerwall.clients")
public class NotificationClientProperties {

    private ServiceClient tracking = new ServiceClient("http://localhost:8084");
    private ServiceClient offer = new ServiceClient("http://localhost:8082");
    private ServiceClient user = new ServiceClient("http://localhost:8081");

    public ServiceClient getTracking() {
        return tracking;
    }

    public void setTracking(ServiceClient tracking) {
        this.tracking = tracking;
    }

    public ServiceClient getOffer() {
        return offer;
    }

    public void setOffer(ServiceClient offer) {
        this.offer = offer;
    }

    public ServiceClient getUser() {
        return user;
    }

    public void setUser(ServiceClient user) {
        this.user = user;
    }

    public static class ServiceClient {
        private String baseUrl;

        public ServiceClient() {
        }

        public ServiceClient(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
