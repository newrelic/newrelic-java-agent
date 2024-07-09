package com.newrelic.agent.superagent;

public interface SuperAgentRelatedDataChangeListener {
    void onHealthDataChange(HealthDataProducer.Type type, String data);
}
