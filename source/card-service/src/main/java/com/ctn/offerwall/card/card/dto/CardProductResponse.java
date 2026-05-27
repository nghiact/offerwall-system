package com.ctn.offerwall.card.card.dto;

import com.ctn.offerwall.card.domain.CardBin;
import com.ctn.offerwall.card.domain.CardProduct;
import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;

import java.util.List;
import java.util.UUID;

public record CardProductResponse(
        UUID id,
        String productCode,
        List<BinResponse> bins,
        List<String> matchedBins,
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

    public static CardProductResponse from(CardProduct product,
                                           String tierLabel,
                                           String displayName,
                                           List<String> matchedBins) {
        return new CardProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getBins().stream().map(BinResponse::from).toList(),
                List.copyOf(matchedBins),
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

    public record BinResponse(UUID id, String bin) {

        public static BinResponse from(CardBin bin) {
            return new BinResponse(bin.getId(), bin.getBin());
        }
    }
}
