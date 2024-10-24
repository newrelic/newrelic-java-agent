/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgentHealthTest {
    @Test
    public void isHealthy_returnsTrue_whenStatusIsHealthy() {
        // Newly constructed AgentHealth object should be healthy
        AgentHealth agentHealth = new AgentHealth(SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos());
        assertTrue(agentHealth.isHealthy());
    }

    @Test
    public void isHealthy_returnsFalse_whenStatusIsNotHealthy() {
        AgentHealth agentHealth = new AgentHealth(SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos());
        agentHealth.setUnhealthyStatus(AgentHealth.Status.INVALID_LICENSE);
        assertFalse(agentHealth.isHealthy());
    }

    @Test
    public void classGetters_returnCorrectValues() {
        long startTime = SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos();
        AgentHealth agentHealth = new AgentHealth(startTime);
        agentHealth.setAgentRunId("runid");
        agentHealth.setUnhealthyStatus(AgentHealth.Status.INVALID_LICENSE);

        assertEquals(startTime, agentHealth.getStartTimeNanos());
        assertEquals("NR-APM-001", agentHealth.getLastError());
        assertEquals("Invalid license key (HTTP status code 401)", agentHealth.getCurrentStatus());
        assertEquals("runid", agentHealth.getAgentRunId());
    }

    @Test
    public void setHealthyStatus_setsStatusToHealthy_whenLastStatusCategoryMatches() {
        AgentHealth agentHealth = new AgentHealth(SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos());
        agentHealth.setUnhealthyStatus(AgentHealth.Status.INVALID_LICENSE);
        agentHealth.setHealthyStatus(AgentHealth.Category.CONFIG);
        assertTrue(agentHealth.isHealthy());
    }

    @Test
    public void setHealthyStatus_ignoresSetStatusToHealthy_whenLastStatusCategoryDoesNotMatch() {
        AgentHealth agentHealth = new AgentHealth(SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos());
        agentHealth.setUnhealthyStatus(AgentHealth.Status.INVALID_LICENSE);
        agentHealth.setHealthyStatus(AgentHealth.Category.HARVEST);
        assertFalse(agentHealth.isHealthy());
    }

    @Test
    public void getDescription_returnsCorrectDescription_whenAdditionalInfoIsProvided() {
        AgentHealth agentHealth = new AgentHealth(SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos());
        agentHealth.setUnhealthyStatus(AgentHealth.Status.HTTP_ERROR, "404", "whatever");
        assertEquals("HTTP error response code [404] received from New Relic while sending data type [whatever]", agentHealth.getCurrentStatus());
    }
}
