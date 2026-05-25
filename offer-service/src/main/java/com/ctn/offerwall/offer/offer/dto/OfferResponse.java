package com.ctn.offerwall.offer.offer.dto;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;
import com.ctn.offerwall.offer.category.dto.OfferCategoryResponse;
import com.ctn.offerwall.offer.domain.Offer;
import com.ctn.offerwall.offer.domain.OfferStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        OfferCategoryResponse category,
        String merchantName,
        String offerSummary,
        String addressDisplay,
        String addressUrl,
        Instant startTime,
        Instant endTime,
        OfferStatus status,
        OfferType offerType,
        OfferEligibilityMode eligibilityMode,
        List<UUID> targetCardProductIds,
        String targetIssuer,
        CardNetwork targetNetwork,
        Integer targetTier,
        CardType targetType,
        Boolean targetPersonal,
        Instant createdAt,
        Instant updatedAt
) {

    public static OfferResponse from(Offer offer, OfferStatus status) {
        return new OfferResponse(
                offer.getId(),
                OfferCategoryResponse.from(offer.getCategory()),
                offer.getMerchantName(),
                offer.getOfferSummary(),
                offer.getAddressDisplay(),
                offer.getAddressUrl(),
                offer.getStartTime(),
                offer.getEndTime(),
                status,
                offer.getOfferType(),
                offer.getEligibilityMode(),
                offer.getTargetCardProductIds().stream()
                        .sorted(Comparator.comparing(UUID::toString))
                        .toList(),
                offer.getTargetIssuer(),
                offer.getTargetNetwork(),
                offer.getTargetTier(),
                offer.getTargetType(),
                offer.getTargetPersonal(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}
