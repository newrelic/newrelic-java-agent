/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import org.junit.Test;

import com.newrelic.api.agent.Agent;

public class BridgeUtilsTest {

    @Test
    public void getTracedMethod() throws NoSuchMethodException, SecurityException {
        Agent.class.getMethod(BridgeUtils.GET_TRACED_METHOD_METHOD_NAME);
    }

    @Test
    public void getTransactionMethod() throws NoSuchMethodException, SecurityException {
        Agent.class.getMethod(BridgeUtils.GET_TRANSACTION_METHOD_NAME);
    }

}
