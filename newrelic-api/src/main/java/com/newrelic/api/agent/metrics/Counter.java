package com.newrelic.api.agent.metrics;

import java.util.Collections;
import java.util.Map;

public interface Counter {
    default void add(long value) {
        add(value, Collections.emptyMap());
    }
    void add(long value, Map<String, ?> attributes);
}
