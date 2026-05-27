package com.ctn.offerwall.user.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "app_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_users_email", columnNames = "email")
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_value", nullable = false)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_roles_user_role", columnNames = {"user_id", "role"})
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Set<UserRole> roles = new LinkedHashSet<>();

    @Embedded
    private NotificationPreferences notificationPreferences = new NotificationPreferences();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String email, String password, NotificationPreferences notificationPreferences) {
        this.email = email;
        this.password = password;
        this.notificationPreferences = notificationPreferences;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean passwordMatches(String candidate) {
        return password.equals(candidate);
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public void addRole(UserRole role) {
        roles.add(role);
    }

    public NotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public void updateNotificationPreferences(boolean emailEnabled, boolean inAppEnabled) {
        notificationPreferences = new NotificationPreferences(emailEnabled, inAppEnabled);
    }
}
