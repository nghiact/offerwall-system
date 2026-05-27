package com.ctn.offerwall.notification.event;

import java.util.UUID;

public interface TrackingEventClient {

    BusinessEventSnapshot getEvent(UUID eventId);
}
