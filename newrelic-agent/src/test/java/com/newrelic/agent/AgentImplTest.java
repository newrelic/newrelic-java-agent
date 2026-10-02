/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.TracerFlags;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentImplTest {
    private final AgentImpl agentImpl = new AgentImpl(AgentBridge.getAgent().getLogger());

    @BeforeClass
    public static void setup() throws Exception {
        createServiceManager(createConfigMap());
    }

    @Test
    public void testAsyncTracerShouldNotStartTxn() {
        TransactionActivity txa = TransactionActivity.create(null, 0);
        ClassMethodSignature sig = new ClassMethodSignature("class", "method", "methodDesc");
        OtherRootTracer tracer = new OtherRootTracer(txa, sig, null, null, TracerFlags.ASYNC, System.nanoTime());
        txa.tracerStarted(tracer);
        Transaction txn = agentImpl.getTransaction();
        Assert.assertTrue(txn instanceof NoOpTransaction);
        Assert.assertFalse(txn instanceof com.newrelic.agent.Transaction);
    }

    @Test
    public void testNoTransaction() {
        com.newrelic.agent.Transaction.clearTransaction();
        agentImpl.getTransaction();
        Assert.assertNull(com.newrelic.agent.Transaction.getTransaction(false));
    }

    @Test
    public void testGetServiceMetadataDelegatesToRPMService() {
        Map<String, String> expected = new HashMap<>();
        expected.put("entity.guid", "guid-1");
        expected.put("tags.region", "us-east-1");

        IRPMService rpmService = mock(IRPMService.class);
        when(rpmService.getServiceMetadata()).thenReturn(expected);

        RPMServiceManager rpmServiceManager = mock(RPMServiceManager.class);
        when(rpmServiceManager.getRPMService()).thenReturn(rpmService);

        MockServiceManager serviceManager = (MockServiceManager) ServiceFactory.getServiceManager();
        RPMServiceManager savedRPMServiceManager = serviceManager.getRPMServiceManager();
        try {
            serviceManager.setRPMServiceManager(rpmServiceManager);
            Map<String, String> actual = agentImpl.getServiceMetadata();
            Assert.assertEquals(expected, actual);
        } finally {
            serviceManager.setRPMServiceManager(savedRPMServiceManager);
        }
    }

    @Test
    public void testGetServiceMetadataReturnsEmptyMapOnException() {
        RPMServiceManager rpmServiceManager = mock(RPMServiceManager.class);
        when(rpmServiceManager.getRPMService()).thenThrow(new RuntimeException("connect manager unavailable"));

        MockServiceManager serviceManager = (MockServiceManager) ServiceFactory.getServiceManager();
        RPMServiceManager savedRPMServiceManager = serviceManager.getRPMServiceManager();
        try {
            serviceManager.setRPMServiceManager(rpmServiceManager);
            Map<String, String> actual = agentImpl.getServiceMetadata();
            Assert.assertNotNull(actual);
            Assert.assertTrue(actual.isEmpty());
        } finally {
            serviceManager.setRPMServiceManager(savedRPMServiceManager);
        }
    }

    @Test
    public void testGetConfiguredCustomLogAttributesDelegatesToConfig() throws Exception {
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("env", "prod");
        Map<String, Object> forwarding = new HashMap<>();
        forwarding.put("custom_attributes", customAttributes);
        Map<String, Object> applicationLogging = new HashMap<>();
        applicationLogging.put("forwarding", forwarding);
        Map<String, Object> map = new HashMap<>();
        map.put("application_logging", applicationLogging);

        MockServiceManager serviceManager = (MockServiceManager) ServiceFactory.getServiceManager();
        ConfigService savedConfigService = serviceManager.getConfigService();
        try {
            AgentConfig agentConfig = AgentHelper.createAgentConfig(true, map, Collections.<String, Object>emptyMap());
            ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
            serviceManager.setConfigService(configService);

            Map<String, Object> actual = agentImpl.getConfiguredCustomLogAttributes();
            Assert.assertEquals(Collections.singletonMap("env", "prod"), actual);
        } finally {
            serviceManager.setConfigService(savedConfigService);
        }
    }

    @Test
    public void testGetConfiguredCustomLogAttributesReturnsEmptyMapWhenNoneConfigured() {
        Map<String, Object> actual = agentImpl.getConfiguredCustomLogAttributes();
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.isEmpty());
    }

    private static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        return map;
    }

    private static void createServiceManager(Map<String, Object> map) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, map, Collections.<String, Object>emptyMap());

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);
    }

}
