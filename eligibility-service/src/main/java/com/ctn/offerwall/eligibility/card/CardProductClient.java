package com.ctn.offerwall.eligibility.card;

import java.util.List;
import java.util.UUID;

public interface CardProductClient {

    List<CardProductSummary> lookupProducts(List<UUID> cardProductIds);
}
