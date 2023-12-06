package com.newrelic.api.agent.metrics;

import java.util.Collections;
import java.util.Map;

/**
 * Summaries are dimensional metrics that track count, total, min and max.
 */
public interface Summary {
    /**
     * Records a value.
     */
    default void record(double value) {
        record(value, Collections.emptyMap());
    }

    /**
     * Records a value with the given attributes.
     */
    void record(double value, Map<String, ?> attributes);
}
