package com.ctn.offerwall.notification.recipient;

import java.util.List;

public interface OfferNotificationRecipientResolver {

    List<OfferNotificationRecipientCandidate> resolveRecipients(OfferNotificationRecipientQuery query);
}
