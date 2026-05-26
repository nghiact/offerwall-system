package com.ctn.offerwall.eligibility.card;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;

import java.util.UUID;

public record CardProductSummary(
        UUID id,
        String productCode,
        String issuer,
        String name,
        CardNetwork network,
        int tier,
        String tierLabel,
        String tierLabelOverride,
        CardType type,
        boolean personal,
        String displayName
) {
}
