package com.ctn.offerwall.user.notification.dto;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import com.ctn.offerwall.common.offer.OfferEligibilityMode;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OfferNotificationRecipientRequest(
        @NotNull
        UUID offerId,

        @NotNull
        UUID sourceEventId,

        @NotNull
        OfferEligibilityMode eligibilityMode,

        List<UUID> targetCardProductIds,

        String targetIssuer,

        CardNetwork targetNetwork,

        Integer targetTier,

        CardType targetType,

        Boolean targetPersonal
) {

    public OfferNotificationRecipientRequest {
        targetCardProductIds = targetCardProductIds == null ? List.of() : List.copyOf(targetCardProductIds);
    }
}
