/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

public interface HealthDataChangeListener {
    void onUnhealthyStatus(AgentHealth.Status newStatus, String... additionalInfo);

    void onHealthyStatus(AgentHealth.Category... category);
}
