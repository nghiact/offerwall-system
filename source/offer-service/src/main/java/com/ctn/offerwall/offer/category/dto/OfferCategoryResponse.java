package com.ctn.offerwall.offer.category.dto;

import com.ctn.offerwall.offer.domain.OfferCategory;

import java.time.Instant;
import java.util.UUID;

public record OfferCategoryResponse(
        UUID id,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public static OfferCategoryResponse from(OfferCategory category) {
        return new OfferCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
