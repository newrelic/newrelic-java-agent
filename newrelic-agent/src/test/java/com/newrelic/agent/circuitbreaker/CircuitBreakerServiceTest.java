/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.circuitbreaker;

import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.AgentImpl;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactoryTest;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.CircuitBreakerConfig;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CircuitBreakerServiceTest {

    private CircuitBreakerService circuitBreaker;

    @BeforeClass
    public static void setup() {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        try{
            serviceManager.start();
        } catch(Exception e){
            throw new RuntimeException(e);
        }

        AgentBridge.instrumentation = new InstrumentationImpl(Agent.LOG);
        AgentBridge.agent = new AgentImpl(Agent.LOG);
    }

    @After
    public void cleanup() throws Exception{
        circuitBreaker.stop();
        TransactionActivity.clear();
        com.newrelic.agent.Transaction.clearTransaction();
    }

    @Before
    public void beforeTest() throws Exception {
        AgentHelper.initializeConfig();
        circuitBreaker = new CircuitBreakerService();
        // without this the default lastChecked values will be zero and the cb won't trip
        circuitBreaker.setPreviousChecksForTesting(-1l, -100l);
        ((MockServiceManager)ServiceFactory.getServiceManager()).setCircuitBreakerService(circuitBreaker);
    }

    @Test
    public void crossMemoryThreshold() throws Exception {
        setCBConfig(circuitBreaker, null, 100, null);
        circuitBreaker.checkAndTrip();
        assertNotTripped();
    }

    @Test
    public void crossCpuThreshold() throws Exception {
        setCBConfig(circuitBreaker, null, null, 0);
        circuitBreaker.checkAndTrip();
        assertNotTripped();
    }

    @Test
    public void crossMemoryAndCpuThreshold() throws Exception {
        assertNotTripped();

        setCBConfig(circuitBreaker, true, 100, 0);

        circuitBreaker.checkAndTrip();
        assertTripped();
    }

    /**
     * New Relic api calls should not return null. Even when the circuit breaker is tripped.
     */
    @Test
    public void testNullAPI() throws Exception {
        assertNotTripped();

        setCBConfig(circuitBreaker, true, 100, 0);
        circuitBreaker.checkAndTrip();
        assertTripped();

        // due to the way we swap around NoOps, Impls, and Dummys there is no reliable way to write a good test.
        // This is better than nothing I guess.
        com.newrelic.agent.bridge.Transaction outerTx = AgentBridge.getAgent().getTransaction(true);
        Assert.assertNotNull(outerTx);
    }

    private void assertTripped() {
        Assert.assertTrue(circuitBreaker.isTripped());
    }

    private void assertNotTripped() {
        Assert.assertFalse(circuitBreaker.isTripped());
    }

    public static void setCBConfig(CircuitBreakerService circuitBreaker, Object enabled, Object memoryThreshold,
            Object cpuThreshold) {
        Map<String, Object> cbSettings = new HashMap<>();
        if (null != enabled)
            cbSettings.put(CircuitBreakerConfig.ENABLED, enabled);
        if (null != memoryThreshold)
            cbSettings.put(CircuitBreakerConfig.MEMORY_THRESHOLD, memoryThreshold);
        if (null != cpuThreshold)
            cbSettings.put(CircuitBreakerConfig.GC_CPU_THRESHOLD, cpuThreshold);

        Map<String, Object> agentSettings = AgentConfigFactoryTest.createStagingMap();
        agentSettings.put(CircuitBreakerConfig.PROPERTY_NAME, cbSettings);
        AgentConfig config = AgentConfigImpl.createAgentConfig(agentSettings);

        circuitBreaker.configChanged("does_not_matter", config);
    }
}
