package com.ctn.offerwall.offer.offer.dto;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import com.ctn.offerwall.common.offer.OfferType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfferRequest(
        @NotNull
        UUID categoryId,

        @NotBlank
        @Size(max = 180)
        String merchantName,

        @NotBlank
        @Size(max = 1000)
        String offerSummary,

        @NotBlank
        @Size(max = 500)
        String addressDisplay,

        @Size(max = 1000)
        String addressUrl,

        @NotNull
        Instant startTime,

        @NotNull
        Instant endTime,

        @NotNull
        OfferType offerType,

        @NotNull
        OfferEligibilityMode eligibilityMode,

        List<UUID> targetCardProductIds,

        @Size(max = 160)
        String targetIssuer,

        CardNetwork targetNetwork,

        @Min(0)
        @Max(5)
        Integer targetTier,

        CardType targetType,

        Boolean targetPersonal
) {
}
