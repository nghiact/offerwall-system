package com.ctn.offerwall.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_sessions",
        indexes = {
                @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_user_sessions_access_expires_at", columnList = "access_token_expires_at"),
                @Index(name = "idx_user_sessions_refresh_expires_at", columnList = "refresh_token_expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_sessions_access_token_hash", columnNames = "access_token_hash"),
                @UniqueConstraint(name = "uk_user_sessions_refresh_token_hash", columnNames = "refresh_token_hash")
        }
)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "access_token_hash", nullable = false, length = 64)
    private String accessTokenHash;

    @Column(name = "access_token_expires_at", nullable = false)
    private Instant accessTokenExpiresAt;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at", nullable = false)
    private Instant refreshTokenExpiresAt;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant revokedAt;

    protected UserSession() {
    }

    public UserSession(AppUser user,
                       String accessTokenHash,
                       Instant accessTokenExpiresAt,
                       String refreshTokenHash,
                       Instant refreshTokenExpiresAt,
                       String userAgent) {
        this.user = user;
        this.accessTokenHash = accessTokenHash;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 512));
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public AppUser getUser() {
        return user;
    }

    public boolean canAccess(Instant now) {
        return revokedAt == null && accessTokenExpiresAt.isAfter(now);
    }

    public boolean canRefresh(Instant now) {
        return revokedAt == null && refreshTokenExpiresAt.isAfter(now);
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
