package com.newrelic.api.agent.metrics;

import java.util.Collections;
import java.util.Map;

public interface Summary {
    default void add(double value) {
        add(value, Collections.emptyMap());
    }
    void add(double value, Map<String, ?> attributes);
}
