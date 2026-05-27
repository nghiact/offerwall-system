package com.ctn.offerwall.user.card;

import com.ctn.offerwall.user.card.dto.CardProductSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CardProductClient {

    List<CardProductSummaryResponse> lookupProducts(List<UUID> cardProductIds);
}
