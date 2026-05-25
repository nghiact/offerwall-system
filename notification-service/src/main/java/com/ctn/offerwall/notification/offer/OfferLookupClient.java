package com.ctn.offerwall.notification.offer;

import java.util.UUID;

public interface OfferLookupClient {

    OfferSnapshot getOffer(UUID offerId);
}
