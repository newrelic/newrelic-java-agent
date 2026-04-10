package com.newrelic.agent.agentcontrol.effectiveconfig;

import java.util.Map;

public class AgentControlIntegrationEffectiveConfigNoOpClient implements AgentControlIntegrationEffectiveConfigClient {
    @Override
    public void sendEffectiveConfigMessage(Map<String, Object> config) {
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
