package com.newrelic.agent.agentcontrol.effectiveconfig;

import java.util.Map;

public interface AgentControlIntegrationEffectiveConfigClient {
    void sendEffectiveConfigMessage(Map<String, Object> config);

    boolean isValid();
}
