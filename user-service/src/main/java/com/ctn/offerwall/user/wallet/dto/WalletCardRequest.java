package com.ctn.offerwall.user.wallet.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WalletCardRequest(
        @NotNull
        UUID cardProductId
) {
}
