package com.newrelic.agent.stats.dimensional;

import java.util.Map;

public interface Aggregator {
    Measure getMeasure(Map<String, Object> attributes);
}
