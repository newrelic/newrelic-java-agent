/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.test.agent.circuitbreaker;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionApiImpl;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.circuitbreaker.CircuitBreakerServiceTest;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CircuitBreakerServiceFunctionalTest {
    private static CircuitBreakerService circuitBreaker;

    @BeforeClass
    public static void beforeClass() throws Exception {
        circuitBreaker = ServiceFactory.getServiceManager().getCircuitBreakerService();
    }

    @Test
    public void crossMemoryAndCpuThreshold() throws Exception {
        assertNotTripped();
        CircuitBreakerServiceTest.setCBConfig(circuitBreaker, true, 100, 0);
        // without this the default lastChecked values will be zero and the cb won't trip
        circuitBreaker.setPreviousChecksForTesting(-1l, -100l);
        circuitBreaker.checkAndTrip();
        assertTripped();
    }

    private void assertTripped() {
        Assert.assertTrue(circuitBreaker.isTripped());
        aTransaction(true);
    }

    private void assertNotTripped() {
        Assert.assertFalse(circuitBreaker.isTripped());
        aTransaction(false);
    }

    @Trace(dispatcher = true)
    private void aTransaction(boolean breakerTripped) {

        Transaction transaction = NewRelic.getAgent().getTransaction();
        InstrumentationImpl impl = new InstrumentationImpl(Agent.LOG);

        if (breakerTripped) {
            ExitTracer createdTracer = impl.createTracer(null, 0, "metricName", TracerFlags.DISPATCHER);
            Assert.assertNull(createdTracer);
            Assert.assertTrue(transaction instanceof NoOpTransaction);
        }
        else {
            Assert.assertTrue(transaction instanceof TransactionApiImpl);
        }
    }
}
