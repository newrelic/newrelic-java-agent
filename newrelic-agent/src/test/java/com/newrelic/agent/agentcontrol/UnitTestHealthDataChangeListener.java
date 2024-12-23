package com.newrelic.agent.agentcontrol;

public class UnitTestHealthDataChangeListener implements HealthDataChangeListener {
    private AgentHealth.Status lastStatus = null;
    private AgentHealth.Category lastCategory = null;

    @Override
    public void onUnhealthyStatus(AgentHealth.Status newStatus, String... additionalInfo) {
        lastStatus = newStatus;
    }

    @Override
    public void onHealthyStatus(AgentHealth.Category... category) {
        lastCategory = category[0];
    }
}
