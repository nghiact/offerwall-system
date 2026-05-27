package com.ctn.offerwall.user.eligibility.dto;

import java.util.List;
import java.util.UUID;

public record UserWalletCandidate(
        UUID userId,
        List<UUID> cardProductIds
) {

    public UserWalletCandidate {
        cardProductIds = cardProductIds == null ? List.of() : List.copyOf(cardProductIds);
    }
}
