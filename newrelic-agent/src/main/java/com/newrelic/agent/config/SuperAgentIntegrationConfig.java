/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import java.net.URI;

public interface SuperAgentIntegrationConfig {
    /**
     * Check if the Super Agent integration service is enabled
     *
     * @return <code>true</code> if the Super Agent Health Check service is enabled, else <code>false</code>.
     */
    boolean isEnabled();

    /**
     * Get the domain socket listener address
     *
     * @return the domain socket address for the health check
     */
    URI getHealthDeliveryLocation();

    /**
     * Return the frequency of the health messages sent to the Super Agent, in seconds
     *
     * @return the health check frequency, in seconds
     */
    int getHealthReportingFrequency();

    /**
     * Return the fleet id assigned by the super agent
     *
     * @return the fleet id, if available
     */
    String getFleetId();

    /**
     * Return the health client type ("file" or "noop" for example)
     *
     * @return the client type
     */
    String getHealthClientType();
}
