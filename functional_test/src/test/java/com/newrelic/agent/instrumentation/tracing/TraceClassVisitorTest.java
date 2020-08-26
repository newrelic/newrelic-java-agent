/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.NoOpInstrumentation;
import com.newrelic.api.agent.Trace;

public class TraceClassVisitorTest {
    public Instrumentation bridgeInstrumentation = AgentBridge.instrumentation;
    public NoOpCountingInstrumentation counter = new NoOpCountingInstrumentation();

    @Before
    public void before() {
        AgentBridge.instrumentation = counter;
    }

    @After
    public void after() {
        AgentBridge.instrumentation = bridgeInstrumentation;
    }

    @Test
    public void testTrace() {
        ClassToTrace ctt = new ClassToTrace();
        Assert.assertEquals(ctt.invokeCount, counter.tracerCount);
        ctt.aMethodToTrace();
        Assert.assertEquals(ctt.invokeCount, counter.tracerCount);
        ctt.aMethodToTrace();
        Assert.assertEquals(ctt.invokeCount, counter.tracerCount);
    }

    @Test
    public void testBypassBridgeTrace() {
        AnImpl impl = new AnImpl();
        Assert.assertEquals(impl.getInvokeCount(), counter.tracerCount);
        impl.aMethodToTrace("foo");
        Assert.assertEquals(impl.getInvokeCount(), counter.tracerCount);
        impl.aMethodToTrace("foo");
        Assert.assertEquals(impl.getInvokeCount(), counter.tracerCount);

    }

    @Test
    public void testBridgeTrace() {
        AnInterface ai = new AnImpl();
        ai.aMethodToTrace("bar");
        Assert.assertEquals(ai.getInvokeCount(), counter.tracerCount);
        ai.aMethodToTrace("bar");
        Assert.assertEquals(ai.getInvokeCount(), counter.tracerCount);
        ai.aMethodToTrace("bar");
        Assert.assertEquals(ai.getInvokeCount(), counter.tracerCount);
    }

    public static class NoOpCountingInstrumentation extends NoOpInstrumentation {
        public int tracerCount = 0;

        @Override
        public ExitTracer createTracer(Object invocationTarget, int signatureId, String metricName, int flags, String instrumentationModule) {
            tracerCount++;
            return super.createTracer(invocationTarget, signatureId, metricName, flags, instrumentationModule);
        }
    }

    public static class ClassToTrace {
        public int invokeCount = 0;

        @Trace
        public void aMethodToTrace() {
            invokeCount++;
        }
    }

    public static interface AnInterface<E> {
        public int getInvokeCount();
        public void aMethodToTrace(E e);
    }

    public static class AnImpl implements AnInterface<String> {
        public int invokeCount = 0;

        @Override
        public int getInvokeCount() {
            return invokeCount;
        }

        @Trace
        @Override
        public void aMethodToTrace(String e) {
            invokeCount++;
        }
    }

}
