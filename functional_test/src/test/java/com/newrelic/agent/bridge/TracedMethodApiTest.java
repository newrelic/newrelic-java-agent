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

import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;

public class TracedMethodApiTest {

    private MockTracer tracer;
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
        tracer = null;
        AgentBridge.instrumentation = new Api();
    }

    @Trace
    @Test
    public void testSetMetricName() {
        Assert.assertNotNull(tracer);
        NewRelic.getAgent().getTracedMethod().setMetricName("testing", "dude");

        Assert.assertNotNull(tracer.getMetricName());
        Assert.assertEquals(2, tracer.getMetricNames().length);
        Assert.assertEquals("testing", tracer.getMetricNames()[0]);
    }

    private final class Api extends InstrumentationImpl {

        public Api() {
            super(NewRelic.getAgent().getLogger());
        }

        @Override
        public ExitTracer createTracer(Object invocationTarget, int signatureId, boolean dispatcher, String metricName,
                String tracerFactoryName, Object[] args) {
            tracer = new MockTracer();
            return tracer;
        }

        @Override
        public ExitTracer createTracer(Object invocationTarget, int signatureId, String metricName, int flags) {
            tracer = new MockTracer();
            return tracer;
        }

    }
}
