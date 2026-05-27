package com.ctn.offerwall.card.domain;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "card_products",
        indexes = {
                @Index(name = "idx_card_products_issuer", columnList = "issuer"),
                @Index(name = "idx_card_products_network", columnList = "network")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_card_products_product_code", columnNames = "product_code")
)
public class CardProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_code", nullable = false, length = 128, unique = true)
    private String productCode;

    @Column(nullable = false, length = 128)
    private String issuer;

    @Column(length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CardNetwork network;

    @Column(nullable = false)
    private int tier;

    @Column(length = 64)
    private String tierLabelOverride;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CardType type;

    @Column(nullable = false)
    private boolean personal = true;

    @OneToMany(mappedBy = "cardProduct", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("bin asc")
    private List<CardBin> bins = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CardProduct() {
    }

    public CardProduct(String productCode,
                       String issuer,
                       String name,
                       CardNetwork network,
                       int tier,
                       String tierLabelOverride,
                       CardType type,
                       boolean personal,
                       Collection<String> bins) {
        update(productCode, issuer, name, network, tier, tierLabelOverride, type, personal, bins);
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

    public void update(String productCode,
                       String issuer,
                       String name,
                       CardNetwork network,
                       int tier,
                       String tierLabelOverride,
                       CardType type,
                       boolean personal,
                       Collection<String> bins) {
        this.productCode = productCode;
        this.issuer = issuer;
        this.name = name;
        this.network = network;
        this.tier = tier;
        this.tierLabelOverride = tierLabelOverride;
        this.type = type;
        this.personal = personal;
        replaceBins(bins);
    }

    public void replaceBins(Collection<String> bins) {
        Set<String> desiredBins = new LinkedHashSet<>(bins);
        this.bins.removeIf(existing -> !desiredBins.contains(existing.getBin()));
        desiredBins.stream()
                .filter(bin -> this.bins.stream().noneMatch(existing -> existing.getBin().equals(bin)))
                .forEach(bin -> this.bins.add(new CardBin(this, bin)));
    }

    public UUID getId() {
        return id;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getName() {
        return name;
    }

    public CardNetwork getNetwork() {
        return network;
    }

    public int getTier() {
        return tier;
    }

    public String getTierLabelOverride() {
        return tierLabelOverride;
    }

    public CardType getType() {
        return type;
    }

    public boolean isPersonal() {
        return personal;
    }

    public List<CardBin> getBins() {
        return bins;
    }
}
