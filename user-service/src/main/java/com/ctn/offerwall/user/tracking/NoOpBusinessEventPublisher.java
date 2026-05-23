package com.ctn.offerwall.user.tracking;

import com.ctn.offerwall.common.event.BusinessEvent;
import org.springframework.stereotype.Component;

@Component
public class NoOpBusinessEventPublisher implements BusinessEventPublisher {

    @Override
    public void publish(BusinessEvent event) {
        // Tracking transport is added when tracking-service exposes ingestion.
    }
}
