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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_cards",
        indexes = {
                @Index(name = "idx_wallet_cards_user_id", columnList = "user_id"),
                @Index(name = "idx_wallet_cards_card_id", columnList = "card_id")
        }
)
public class WalletCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "card_id", nullable = false, length = 64)
    private String cardId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletCard() {
    }

    public WalletCard(AppUser user, String cardId) {
        this.user = user;
        this.cardId = cardId;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getCardId() {
        return cardId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
