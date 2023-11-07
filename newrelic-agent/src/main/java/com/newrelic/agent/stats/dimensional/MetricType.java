package com.newrelic.agent.stats.dimensional;

enum MetricType {
    count, summary;

    public String attributeName() {
        return "metric." + name();
    }
}
