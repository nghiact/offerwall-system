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
        List<String> targetIssuers,
        CardNetwork targetNetwork,
        List<CardNetwork> targetNetworks,
        Integer targetTier,
        CardType targetType,
        List<CardType> targetTypes,
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
                firstOrNull(offer.getTargetIssuers().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()),
                offer.getTargetIssuers().stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList(),
                firstOrNull(offer.getTargetNetworks().stream()
                        .sorted(Comparator.comparing(CardNetwork::name))
                        .toList()),
                offer.getTargetNetworks().stream()
                        .sorted(Comparator.comparing(CardNetwork::name))
                        .toList(),
                offer.getTargetTier(),
                firstOrNull(offer.getTargetTypes().stream()
                        .sorted(Comparator.comparing(CardType::name))
                        .toList()),
                offer.getTargetTypes().stream()
                        .sorted(Comparator.comparing(CardType::name))
                        .toList(),
                offer.getTargetPersonal(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }

    private static <T> T firstOrNull(List<T> values) {
        return values.isEmpty() ? null : values.getFirst();
    }
}
