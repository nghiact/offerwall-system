package com.ctn.offerwall.card.card.dto;

import com.ctn.offerwall.card.domain.CardProduct;
import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;

import java.util.UUID;

public record CardProductSummaryResponse(
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

    public static CardProductSummaryResponse from(CardProduct product, String tierLabel, String displayName) {
        return new CardProductSummaryResponse(
                product.getId(),
                product.getProductCode(),
                product.getIssuer(),
                product.getName(),
                product.getNetwork(),
                product.getTier(),
                tierLabel,
                product.getTierLabelOverride(),
                product.getType(),
                product.isPersonal(),
                displayName
        );
    }
}
