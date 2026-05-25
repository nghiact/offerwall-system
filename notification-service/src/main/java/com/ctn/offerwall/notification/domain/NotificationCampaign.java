package com.ctn.offerwall.notification.domain;

import com.ctn.offerwall.common.notification.NotificationPriority;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "notification_campaigns",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_campaigns_source_event_id",
                columnNames = "source_event_id"
        )
)
public class NotificationCampaign {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_mode", nullable = false, length = 20)
    private NotificationSendMode sendMode;

    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(name = "source_event_id")
    private UUID sourceEventId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt asc")
    private List<NotificationDelivery> deliveries = new ArrayList<>();

    protected NotificationCampaign() {
    }

    public NotificationCampaign(String title,
                                String body,
                                NotificationPriority priority,
                                NotificationSendMode sendMode,
                                Instant scheduledFor,
                                UUID offerId,
                                UUID sourceEventId) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.body = body;
        this.priority = priority;
        this.sendMode = sendMode;
        this.scheduledFor = scheduledFor;
        this.offerId = offerId;
        this.sourceEventId = sourceEventId;
    }

    public void addDelivery(NotificationDelivery delivery) {
        deliveries.add(delivery);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public NotificationSendMode getSendMode() {
        return sendMode;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<NotificationDelivery> getDeliveries() {
        return deliveries;
    }
}
