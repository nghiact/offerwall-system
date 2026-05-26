package com.ctn.offerwall.notification.offer;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfferSnapshot(
        UUID id,
        String merchantName,
        String offerSummary,
        String addressDisplay,
        String addressUrl,
        Instant startTime,
        Instant endTime,
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
        Boolean targetPersonal
) {
}
