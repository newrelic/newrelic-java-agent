package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.agentcontrol.effectiveconfig.AgentControlIntegrationEffectiveConfigClient;

import java.util.Map;

public class AgentControlEffectiveConfigUnitTestClient implements AgentControlIntegrationEffectiveConfigClient {
    public Map<String, Object> effectiveConfig = null;

    @Override
    public void sendEffectiveConfigMessage(Map<String, Object> config) {
        this.effectiveConfig = config;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
