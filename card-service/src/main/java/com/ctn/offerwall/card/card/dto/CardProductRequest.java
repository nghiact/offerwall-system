package com.ctn.offerwall.card.card.dto;

import com.ctn.offerwall.common.card.CardNetwork;
import com.ctn.offerwall.common.card.CardType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CardProductRequest(
        @NotBlank String productCode,
        @NotBlank String issuer,
        String name,
        @NotNull CardNetwork network,
        @Min(0) @Max(5) int tier,
        String tierLabelOverride,
        @NotNull CardType type,
        Boolean personal,
        @NotEmpty List<String> bins
) {
}
