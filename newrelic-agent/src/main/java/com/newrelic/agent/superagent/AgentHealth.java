/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

public class AgentHealth {
    private static final String HEALTHY_DEFAULT = "healthy";

    private boolean isHealthy;
    private String lastError;
    private String currentStatus;

    AgentHealth() {
        this.isHealthy = true;
        this.lastError = "";
        this.currentStatus = HEALTHY_DEFAULT;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    public String getLastError() {
        return lastError;
    }

    void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }
}
