package com.ctn.offerwall.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "offerwall.user.defaults")
public class UserDefaultsProperties {

    private boolean notificationEmailEnabled;
    private boolean notificationInAppEnabled = true;

    public boolean isNotificationEmailEnabled() {
        return notificationEmailEnabled;
    }

    public void setNotificationEmailEnabled(boolean notificationEmailEnabled) {
        this.notificationEmailEnabled = notificationEmailEnabled;
    }

    public boolean isNotificationInAppEnabled() {
        return notificationInAppEnabled;
    }

    public void setNotificationInAppEnabled(boolean notificationInAppEnabled) {
        this.notificationInAppEnabled = notificationInAppEnabled;
    }
}
