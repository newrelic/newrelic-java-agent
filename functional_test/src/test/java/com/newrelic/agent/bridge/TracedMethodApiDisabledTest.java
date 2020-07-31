/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;

public class TracedMethodApiDisabledTest {

    private static PrivateApi originalPrivateApi;

    @BeforeClass
    public static void setup() {
        originalPrivateApi = AgentBridge.privateApi;
    }

    @AfterClass
    public static void teardown() {
        AgentBridge.privateApi = originalPrivateApi;
    }

    @Before
    public void before() {
        AgentBridge.privateApi = new NoOpPrivateApi();
    }

    @Trace
    @Test
    public void testSetMetricName() {
        Assert.assertNotNull(NewRelic.getAgent().getTracedMethod());
        // just testing that this doesn't throw an NPE
        NewRelic.getAgent().getTracedMethod().setMetricName("testing", "dude");
    }

    @Trace
    @Test
    public void testNoOpTracedMethod() {
        Assert.assertEquals(NoOpTracedMethod.class.getSimpleName(),
                NewRelic.getAgent().getTracedMethod().getMetricName());
    }
}
