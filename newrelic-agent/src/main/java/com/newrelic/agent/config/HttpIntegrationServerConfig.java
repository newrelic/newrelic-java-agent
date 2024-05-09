/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

public interface HttpIntegrationServerConfig {
    boolean isEnabled();

    void setEnabled(boolean enabled);

    int getPort();
}
