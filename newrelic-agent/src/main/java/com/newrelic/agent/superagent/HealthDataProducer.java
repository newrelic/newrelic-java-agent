package com.newrelic.agent.superagent;

public interface HealthDataProducer {
    enum Type {
        InstanceId,
        Error,
    }

    void registerHealthDataChangeListener(SuperAgentRelatedDataChangeListener listener);
}
