package com.ctn.offerwall.card.card.dto;

import java.util.List;
import java.util.UUID;

public record CardProductLookupRequest(
        List<UUID> cardProductIds
) {

    public CardProductLookupRequest {
        cardProductIds = cardProductIds == null ? List.of() : List.copyOf(cardProductIds);
    }
}
