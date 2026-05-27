package com.ctn.offerwall.user.wallet.dto;

import com.ctn.offerwall.user.card.dto.CardProductSummaryResponse;
import com.ctn.offerwall.user.domain.WalletCard;

import java.time.Instant;
import java.util.UUID;

public record WalletCardResponse(
        UUID id,
        UUID cardProductId,
        Instant createdAt,
        CardProductSummaryResponse cardProduct
) {

    public static WalletCardResponse from(WalletCard walletCard) {
        return from(walletCard, null);
    }

    public static WalletCardResponse from(WalletCard walletCard, CardProductSummaryResponse cardProduct) {
        return new WalletCardResponse(
                walletCard.getId(),
                parseCardProductId(walletCard.getCardId()),
                walletCard.getCreatedAt(),
                cardProduct
        );
    }

    private static UUID parseCardProductId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
