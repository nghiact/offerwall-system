package com.ctn.offerwall.common.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EventMetadata(Map<String, String> values) {

    private static final EventMetadata EMPTY = new EventMetadata(Map.of());

    public EventMetadata {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static EventMetadata empty() {
        return EMPTY;
    }

    public static EventMetadata of(String key, String value) {
        return new EventMetadata(Map.of(key, value));
    }

    public String get(String key) {
        return values.get(key);
    }
}
