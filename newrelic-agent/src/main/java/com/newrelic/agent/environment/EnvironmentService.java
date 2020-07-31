/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.environment;

import com.newrelic.agent.service.Service;

/**
 * The interface for the environment service.
 */
public interface EnvironmentService extends Service {

    /**
     * Get the process id.
     */
    int getProcessPID();

    /**
     * Get the application server environment.
     */
    Environment getEnvironment();

}
