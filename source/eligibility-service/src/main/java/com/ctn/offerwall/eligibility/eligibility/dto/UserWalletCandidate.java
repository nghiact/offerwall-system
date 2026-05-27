package com.ctn.offerwall.eligibility.eligibility.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UserWalletCandidate(
        @NotNull
        UUID userId,

        List<UUID> cardProductIds
) {

    public UserWalletCandidate {
        cardProductIds = cardProductIds == null ? List.of() : List.copyOf(cardProductIds);
    }
}
