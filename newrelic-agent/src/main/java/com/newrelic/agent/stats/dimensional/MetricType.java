package com.newrelic.agent.stats.dimensional;

public enum MetricType {
    count, summary;

    public String attributeName() {
        return "metric." + name();
    }
}
