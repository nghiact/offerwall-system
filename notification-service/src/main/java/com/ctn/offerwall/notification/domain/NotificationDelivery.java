package com.ctn.offerwall.notification.domain;

import com.ctn.offerwall.common.notification.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_deliveries",
        indexes = {
                @Index(name = "idx_notification_deliveries_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_notification_deliveries_recipient_user_id", columnList = "recipient_user_id"),
                @Index(name = "idx_notification_deliveries_status", columnList = "status"),
                @Index(name = "idx_notification_deliveries_channel", columnList = "channel")
        }
)
public class NotificationDelivery {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private NotificationCampaign campaign;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "recipient_email", length = 320)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationDeliveryStatus status;

    @Column(name = "preference_bypassed", nullable = false)
    private boolean preferenceBypassed;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationDelivery() {
    }

    public NotificationDelivery(NotificationCampaign campaign,
                                UUID recipientUserId,
                                String recipientEmail,
                                NotificationChannel channel,
                                NotificationDeliveryStatus status,
                                boolean preferenceBypassed,
                                String failureReason,
                                Instant sentAt) {
        this.id = UUID.randomUUID();
        this.campaign = campaign;
        this.recipientUserId = recipientUserId;
        this.recipientEmail = recipientEmail;
        this.channel = channel;
        this.status = status;
        this.preferenceBypassed = preferenceBypassed;
        this.failureReason = failureReason;
        this.sentAt = sentAt;
    }

    public void markRead(Instant readAt) {
        this.readAt = readAt;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public NotificationCampaign getCampaign() {
        return campaign;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    public boolean isPreferenceBypassed() {
        return preferenceBypassed;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
