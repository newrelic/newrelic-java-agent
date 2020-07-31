/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.environment;

/**
 * @see Environment#addEnvironmentChangeListener(EnvironmentChangeListener)
 */
public interface EnvironmentChangeListener {

    /**
     * This is invoked whenever the identifying environment information changes.
     */
    void agentIdentityChanged(AgentIdentity agentIdentity);
}
