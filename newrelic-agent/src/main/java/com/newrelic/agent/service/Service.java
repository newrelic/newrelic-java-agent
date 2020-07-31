/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.logging.IAgentLogger;

/**
 * A service which starts and stops with the agent.
 */
public interface Service {

    /**
     * Get the service name.
     *
     * @return the name of the service
     */
    String getName();

    /**
     * Start the service.
     */
    void start() throws Exception;

    /**
     * Stop the service.
     */
    void stop() throws Exception;

    /**
     * Is the service enabled.
     *
     * @return <code>true</code> if the service is enabled
     */
    boolean isEnabled();

    IAgentLogger getLogger();

    /**
     * Is the service in the started state {@link ServiceState#STARTED}
     *
     * @return <code>true</code> if the service is started
     */
    boolean isStarted();

    /**
     * Is the service in the stopped state {@link ServiceState#STOPPED}
     *
     * @return <code>true</code> if the service is stopped
     */
    boolean isStopped();

    /**
     * Is the service in the started state {@link ServiceState#STARTED} or starting state {@link ServiceState#STARTING}
     *
     * @return <code>true</code> if the service is started or starting
     */
    boolean isStartedOrStarting();

    /**
     * Is the service in the stopped state {@link ServiceState#STOPPED} or stopping state {@link ServiceState#STOPPING}
     *
     * @return <code>true</code> if the service is stopped or stopping
     */
    boolean isStoppedOrStopping();

}
