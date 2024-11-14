/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.SuperAgentIntegrationConfig;

import java.util.List;

public class SuperAgentIntegrationUtils {
    public static long getPseudoCurrentTimeNanos() {
        // The message expects the time in nanoseconds. Since this is a practical impossibility on most hardware,
        // simply get the current ms and multiply.
        return System.currentTimeMillis() * 1000000;
    }

    public static void reportUnhealthyStatus(List<HealthDataChangeListener> healthDataChangeListeners,
            AgentHealth.Status newStatus, String... additionalInfo) {
        for (HealthDataChangeListener listener : healthDataChangeListeners) {
            listener.onUnhealthyStatus(newStatus, additionalInfo);
        }
    }

    public static void reportHealthyStatus(List<HealthDataChangeListener> healthDataChangeListeners, AgentHealth.Category... categories) {
        for (HealthDataChangeListener listener : healthDataChangeListeners) {
            listener.onHealthyStatus(categories);
        }
    }

    public static void reportUnhealthyStatusPriorToServiceStart(AgentConfig config, AgentHealth.Status status) {
        SuperAgentIntegrationConfig superAgentIntegrationConfig = config.getSuperAgentIntegrationConfig();
        if (superAgentIntegrationConfig.isEnabled()) {
            SuperAgentIntegrationHealthClient client = SuperAgentIntegrationClientFactory.createHealthClient(superAgentIntegrationConfig);
            AgentHealth agentHealth = new AgentHealth(getPseudoCurrentTimeNanos());
            agentHealth.setUnhealthyStatus(status);
            client.sendHealthMessage(agentHealth);
        }
    }
}
