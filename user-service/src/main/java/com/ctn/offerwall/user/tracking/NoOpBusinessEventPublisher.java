package com.ctn.offerwall.user.tracking;

import com.ctn.offerwall.common.event.BusinessEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "offerwall.tracking", name = "enabled", havingValue = "false")
public class NoOpBusinessEventPublisher implements BusinessEventPublisher {

    @Override
    public void publish(BusinessEvent event) {
        // Disabled by local configuration.
    }
}
