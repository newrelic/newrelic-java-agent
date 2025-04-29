/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import java.net.URI;

public interface AgentControlIntegrationConfig {
    /**
     * Check if the Agent Control integration service is enabled
     *
     * @return <code>true</code> if the Agent Control Health Check service is enabled, else <code>false</code>.
     */
    boolean isEnabled();

    /**
     * Get the health reporting location information
     *
     * @return the location info for the health check
     */
    URI getHealthDeliveryLocation();

    /**
     * Return the frequency of the health messages reported to Agent Control, in seconds
     *
     * @return the health check frequency, in seconds
     */
    int getHealthReportingFrequency();

    /**
     * Return the health client type ("file" or "noop" for example)
     *
     * @return the client type
     */
    String getHealthClientType();
}
