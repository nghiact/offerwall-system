package com.ctn.offerwall.card.domain;

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
        name = "card_bins",
        indexes = {
                @Index(name = "idx_card_bins_bin", columnList = "bin"),
                @Index(name = "idx_card_bins_product_id", columnList = "card_product_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_card_bins_product_bin",
                columnNames = {"card_product_id", "bin"}
        )
)
public class CardBin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_product_id", nullable = false)
    private CardProduct cardProduct;

    @Column(nullable = false, length = 12)
    private String bin;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected CardBin() {
    }

    public CardBin(CardProduct cardProduct, String bin) {
        this.cardProduct = cardProduct;
        this.bin = bin;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CardProduct getCardProduct() {
        return cardProduct;
    }

    public String getBin() {
        return bin;
    }
}
