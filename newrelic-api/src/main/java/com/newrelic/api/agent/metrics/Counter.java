package com.newrelic.api.agent.metrics;

import java.util.Collections;
import java.util.Map;

public interface Counter {
    default void add(long increment) {
        add(increment, Collections.emptyMap());
    }
    void add(long increment, Map<String, ?> attributes);
}
