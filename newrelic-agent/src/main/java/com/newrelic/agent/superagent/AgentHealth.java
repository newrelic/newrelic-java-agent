/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

public class AgentHealth {
    public enum Category {
        AGENT, CONFIG, HARVEST, CIRCUIT_BREAKER,
    }

    public enum Status {
        HEALTHY("NR-APM-000", "Healthy", Category.AGENT),
        INVALID_LICENSE("NR-APM-001", "Invalid license key (HTTP status code 401)", Category.CONFIG),
        MISSING_LICENSE("NR-APM-002", "License key missing in configuration", Category.CONFIG),
        FORCED_DISCONNECT("NR-APM-003", "Forced disconnect received from New Relic (HTTP status code 410)", Category.HARVEST),
        HTTP_ERROR("NR-APM-004", "HTTP error response code [%s] received from New Relic while sending data type [%s]", Category.HARVEST),
        MISSING_APP_NAME("NR-APM-005", "Missing application name in agent configuration", Category.CONFIG),
        MAX_APP_NAMES_EXCEEDED("NR-APM-006", "The maximum number of configured app names (3) exceeded", Category.CONFIG),
        PROXY_ERROR("NR-APM-007", "HTTP Proxy configuration error; response code [%s]", Category.HARVEST),
        AGENT_DISABLED("NR-APM-008", "Agent is disabled via configuration", Category.CONFIG),
        SHUTDOWN("NR-APM-099", "Agent has shutdown", Category.AGENT),
        GC_CIRCUIT_BREAKER("NR-APM-100", "Garbage collection circuit breaker triggered: Percent free memory %s; GC CPU time: %s", Category.CIRCUIT_BREAKER),;

        private final String code;
        private final String description;
        private String [] additionalInfo;
        private final Category category;

        Status(String code, String description, Category category) {
            this.code = code;
            this.description = description;
            this.category = category;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            String finalDescription;
            if (additionalInfo != null && additionalInfo.length > 0) {
                finalDescription = String.format(description, (Object[]) additionalInfo);
            } else {
                finalDescription = description;
            }
            return finalDescription;
        }

        public boolean isHealthStatus() {
            return this == HEALTHY;
        }

        void setAdditionalInfo(String[] additionalInfo) {
            this.additionalInfo = additionalInfo;
        }
    }

    private final long startTimeNanos;
    private Status status;
    private String agentRunId;
    AgentHealth(long startTimeNanos) {
        this.startTimeNanos = startTimeNanos;
        status = Status.HEALTHY;
        agentRunId = "";
    }

    void setUnhealthyStatus(Status newStatus, String... additionalInfo) {
        status = newStatus;
        status.setAdditionalInfo(additionalInfo);
    }

    void setHealthyStatus(Category category) {
        //Only set the status to healthy if the category matches the current status category
        if (category == status.category) {
            status = Status.HEALTHY;
        }
    }

    void setAgentRunId(String agentRunId) {
        this.agentRunId = agentRunId;
    }

    public boolean isHealthy() {
        return status.isHealthStatus();
    }

    public String getLastError() {
        return status.getCode();
    }

    public String getCurrentStatus() {
        return status.getDescription();
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public String getAgentRunId() {
        return agentRunId;
    }
}
