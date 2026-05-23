package com.ctn.offerwall.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class NotificationPreferences {

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "in_app_notifications_enabled", nullable = false)
    private boolean inAppEnabled;

    protected NotificationPreferences() {
    }

    public NotificationPreferences(boolean emailEnabled, boolean inAppEnabled) {
        this.emailEnabled = emailEnabled;
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }
}
