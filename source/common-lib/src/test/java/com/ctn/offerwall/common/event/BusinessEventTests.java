package com.ctn.offerwall.common.event;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessEventTests {

    @Test
    void fillsGeneratedFieldsForSuccessEvent() {
        BusinessEvent event = BusinessEvent.success(
                EventType.USER_SIGNED_UP,
                EntityType.USER,
                "user-1",
                "user-1",
                EventMetadata.of("source", "signup")
        );

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.outcome()).isEqualTo(EventOutcome.SUCCESS);
        assertThat(event.metadata().get("source")).isEqualTo("signup");
    }

    @Test
    void metadataIsDefensivelyCopied() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("key", "value");

        EventMetadata metadata = new EventMetadata(values);
        values.put("key", "changed");

        assertThat(metadata.get("key")).isEqualTo("value");
        assertThatThrownBy(() -> metadata.values().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
