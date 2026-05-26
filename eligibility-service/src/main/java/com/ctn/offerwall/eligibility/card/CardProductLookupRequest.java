package com.ctn.offerwall.eligibility.card;

import java.util.List;
import java.util.UUID;

record CardProductLookupRequest(List<UUID> cardProductIds) {
}
