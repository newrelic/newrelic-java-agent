package com.newrelic.agent.superagent;

public interface HealthDataProducer {
    void registerHealthDataChangeListener(HealthDataChangeListener listener);
}
