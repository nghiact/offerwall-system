package com.ctn.offerwall.offer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "offer_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_offer_categories_code", columnNames = "code")
)
public class OfferCategory {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected OfferCategory() {
    }

    public OfferCategory(String code, String name, String description) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public void update(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
