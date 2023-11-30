package com.newrelic.api.agent.metrics;

import java.util.Collections;
import java.util.Map;

/**
 * Counters are used to record long values.
 */
public interface Counter {
    /**
     * Records an increment value.
     */
    default void add(long increment) {
        add(increment, Collections.emptyMap());
    }

    /**
     * Records an increment value with the given attributes.
     */
    void add(long increment, Map<String, ?> attributes);
}
