package com.ctn.offerwall.user.wallet.dto;

import com.ctn.offerwall.user.domain.WalletCard;

import java.time.Instant;
import java.util.UUID;

public record WalletCardResponse(
        UUID id,
        UUID cardProductId,
        Instant createdAt
) {

    public static WalletCardResponse from(WalletCard walletCard) {
        return new WalletCardResponse(
                walletCard.getId(),
                UUID.fromString(walletCard.getCardId()),
                walletCard.getCreatedAt()
        );
    }
}
