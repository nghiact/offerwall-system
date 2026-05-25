package com.ctn.offerwall.offer.domain;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "offers",
        indexes = {
                @Index(name = "idx_offers_category_id", columnList = "category_id"),
                @Index(name = "idx_offers_merchant_name", columnList = "merchant_name"),
                @Index(name = "idx_offers_start_time", columnList = "start_time"),
                @Index(name = "idx_offers_end_time", columnList = "end_time")
        }
)
public class Offer {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private OfferCategory category;

    @Column(name = "merchant_name", nullable = false, length = 180)
    private String merchantName;

    @Column(name = "offer_summary", nullable = false, length = 1000)
    private String offerSummary;

    @Column(name = "address_display", nullable = false, length = 500)
    private String addressDisplay;

    @Column(name = "address_url", length = 1000)
    private String addressUrl;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false, length = 20)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_mode", nullable = false, length = 20)
    private OfferEligibilityMode eligibilityMode;

    @ElementCollection
    @CollectionTable(
            name = "offer_target_card_products",
            joinColumns = @JoinColumn(name = "offer_id"),
            indexes = @Index(name = "idx_offer_target_card_products_card_id", columnList = "card_product_id")
    )
    @Column(name = "card_product_id", nullable = false)
    private Set<UUID> targetCardProductIds = new LinkedHashSet<>();

    @Column(name = "target_issuer", length = 160)
    private String targetIssuer;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_network", length = 20)
    private CardNetwork targetNetwork;

    @Column(name = "target_tier")
    private Integer targetTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private CardType targetType;

    @Column(name = "target_personal")
    private Boolean targetPersonal;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Offer() {
    }

    public Offer(OfferCategory category,
                 String merchantName,
                 String offerSummary,
                 String addressDisplay,
                 String addressUrl,
                 Instant startTime,
                 Instant endTime,
                 OfferType offerType,
                 OfferEligibilityMode eligibilityMode,
                 Set<UUID> targetCardProductIds,
                 String targetIssuer,
                 CardNetwork targetNetwork,
                 Integer targetTier,
                 CardType targetType,
                 Boolean targetPersonal) {
        this.id = UUID.randomUUID();
        update(category, merchantName, offerSummary, addressDisplay, addressUrl, startTime, endTime, offerType,
                eligibilityMode, targetCardProductIds, targetIssuer, targetNetwork, targetTier, targetType, targetPersonal);
    }

    public void update(OfferCategory category,
                       String merchantName,
                       String offerSummary,
                       String addressDisplay,
                       String addressUrl,
                       Instant startTime,
                       Instant endTime,
                       OfferType offerType,
                       OfferEligibilityMode eligibilityMode,
                       Set<UUID> targetCardProductIds,
                       String targetIssuer,
                       CardNetwork targetNetwork,
                       Integer targetTier,
                       CardType targetType,
                       Boolean targetPersonal) {
        this.category = category;
        this.merchantName = merchantName;
        this.offerSummary = offerSummary;
        this.addressDisplay = addressDisplay;
        this.addressUrl = addressUrl;
        this.startTime = startTime;
        this.endTime = endTime;
        this.offerType = offerType;
        this.eligibilityMode = eligibilityMode;
        this.targetCardProductIds.clear();
        this.targetCardProductIds.addAll(targetCardProductIds);
        this.targetIssuer = targetIssuer;
        this.targetNetwork = targetNetwork;
        this.targetTier = targetTier;
        this.targetType = targetType;
        this.targetPersonal = targetPersonal;
    }

    public OfferStatus statusAt(Instant now) {
        if (now.isBefore(startTime)) {
            return OfferStatus.UPCOMING;
        }
        if (!now.isBefore(endTime)) {
            return OfferStatus.ENDED;
        }
        return OfferStatus.ACTIVE;
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

    public OfferCategory getCategory() {
        return category;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getOfferSummary() {
        return offerSummary;
    }

    public String getAddressDisplay() {
        return addressDisplay;
    }

    public String getAddressUrl() {
        return addressUrl;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public OfferType getOfferType() {
        return offerType;
    }

    public OfferEligibilityMode getEligibilityMode() {
        return eligibilityMode;
    }

    public Set<UUID> getTargetCardProductIds() {
        return targetCardProductIds;
    }

    public String getTargetIssuer() {
        return targetIssuer;
    }

    public CardNetwork getTargetNetwork() {
        return targetNetwork;
    }

    public Integer getTargetTier() {
        return targetTier;
    }

    public CardType getTargetType() {
        return targetType;
    }

    public Boolean getTargetPersonal() {
        return targetPersonal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
