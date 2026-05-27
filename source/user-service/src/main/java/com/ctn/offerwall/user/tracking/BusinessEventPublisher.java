package com.ctn.offerwall.user.tracking;

import com.ctn.offerwall.common.event.BusinessEvent;

public interface BusinessEventPublisher {

    void publish(BusinessEvent event);
}
