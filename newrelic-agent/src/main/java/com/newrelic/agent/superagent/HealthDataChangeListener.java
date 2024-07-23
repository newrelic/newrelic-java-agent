package com.newrelic.agent.superagent;

public interface HealthDataChangeListener {
    void onUnhealthyStatus(AgentHealth.Status newStatus, String... additionalInfo);

    void onHealthyStatus(AgentHealth.Category... category);
}
