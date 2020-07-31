/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

public class NoOpJmxApi implements JmxApi {

    @Override
    public void addJmxMBeanGroup(String jmxSetName) {
    }

    @Override
    public void createMBeanServerIfNeeded() {
    }

}
