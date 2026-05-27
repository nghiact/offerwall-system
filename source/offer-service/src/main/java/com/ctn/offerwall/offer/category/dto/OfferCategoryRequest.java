package com.ctn.offerwall.offer.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfferCategoryRequest(
        @NotBlank
        @Size(max = 160)
        String name,

        @Size(max = 500)
        String description
) {
}
