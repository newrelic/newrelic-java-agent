/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

public interface JmxApi {

    void addJmxMBeanGroup(String jmxSetName);

    void createMBeanServerIfNeeded();

}
