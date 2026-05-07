/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.ApplicationNamePriority;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RPMServiceManagerServerlessTest {

    private static final String APP_NAME = "Unit Test";

    @Before
    public void setup() throws Exception {
        createServiceManager(createConfigMap());
    }

    @After
    public void teardown() throws Exception {
        ServiceFactory.getServiceManager().stop();
        ServiceFactory.setServiceManager(null);
    }

    private void createServiceManager(Map<String, Object> map) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        RPMServiceManager rpmServiceManager = new RPMServiceManagerImpl() {

            @Override
            protected IRPMService createRPMService(List<String> appNames, ConnectionConfigListener connectionConfigListener,
                    ConnectionListener connectionListener) {
                MockRPMService rpmService = new MockRPMService();
                rpmService.setApplicationNames(appNames);
                rpmService.setConnectionConfigListener(connectionConfigListener);
                rpmService.setConnectionListener(connectionListener);
                return rpmService;
            }

        };
        serviceManager.setRPMServiceManager(rpmServiceManager);
    }

    private Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("serverless_mode", Collections.singletonMap("enabled", true));
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        return map;
    }

    @Test
    public void getRPMService() throws Exception {

        RPMServiceManager rpmServiceManager = ServiceFactory.getRPMServiceManager();

        IRPMService rpmService1 = rpmServiceManager.getRPMService();
        Assert.assertEquals(APP_NAME, rpmService1.getApplicationName());

        IRPMService rpmService = rpmServiceManager.getRPMService(APP_NAME);
        Assert.assertEquals(APP_NAME, rpmService.getApplicationName());
        Assert.assertSame(rpmService1, rpmService);

        rpmService = rpmServiceManager.getRPMService();
        Assert.assertEquals(APP_NAME, rpmService.getApplicationName());
        Assert.assertSame(rpmService1, rpmService);

        rpmService = rpmServiceManager.getRPMService(null);
        Assert.assertEquals(APP_NAME, rpmService.getApplicationName());
        Assert.assertSame(rpmService1, rpmService);

        rpmService = rpmServiceManager.getRPMService("Bogus");
        Assert.assertNull(rpmService);

        IRPMService rpmService2 = rpmServiceManager.getOrCreateRPMService("Bogus");
        Assert.assertNotNull(rpmService2);
        Assert.assertEquals("Bogus", rpmService2.getApplicationName());
        Assert.assertNotSame(rpmService1, rpmService2);

        IRPMService rpmService3 = rpmServiceManager.getRPMService("Bogus");
        Assert.assertNotNull(rpmService3);
        Assert.assertEquals("Bogus", rpmService3.getApplicationName());

        Assert.assertSame(rpmService2, rpmService3);
        Assert.assertEquals(rpmServiceManager.getRPMService("Bogus"), rpmServiceManager.getRPMService("Bogus"));
        Assert.assertEquals(rpmServiceManager.getOrCreateRPMService("Bogus"),
                rpmServiceManager.getOrCreateRPMService("Bogus"));

        PriorityApplicationName pan = PriorityApplicationName.create("MyApp1;MyApp2",
                ApplicationNamePriority.REQUEST_ATTRIBUTE);
        IRPMService rpmService4 = rpmServiceManager.getOrCreateRPMService(pan);
        Assert.assertNotNull(rpmService4);
        Assert.assertEquals("MyApp1", rpmService4.getApplicationName());
        List<String> appNames = ((MockRPMService) rpmService4).getApplicationNames();
        Assert.assertEquals(2, appNames.size());
        Assert.assertEquals("MyApp1", appNames.get(0));
        Assert.assertEquals("MyApp2", appNames.get(1));
    }

    @Test
    public void connectionListener() throws Exception {

        RPMServiceManager rpmServiceManager = ServiceFactory.getRPMServiceManager();
        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();

        final AtomicReference<IRPMService> connectedService = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> data = new AtomicReference<>();
        ConnectionConfigListener connectionConfigListener = new ConnectionConfigListener() {
            @Override
            public AgentConfig connected(IRPMService rpmService, Map<String, Object> connectionInfo) {
                connectedService.set(rpmService);
                data.set(connectionInfo);
                return AgentConfigImpl.createAgentConfig(connectionInfo);
            }
        };
        rpmServiceManager.setConnectionConfigListener(connectionConfigListener);

        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("TestKey", "TestValue");
        AgentConfig config = rpmService.getConnectionConfigListener().connected(rpmService, connectionInfo);
        rpmService.getConnectionListener().connected(rpmService, config);
        Assert.assertNotNull(connectedService.get());
        Assert.assertEquals(APP_NAME, connectedService.get().getApplicationName());
        Assert.assertSame(rpmService, connectedService.get());
        Assert.assertEquals(1, data.get().size());
        Assert.assertEquals("TestValue", data.get().get("TestKey"));

        rpmService.getConnectionListener().disconnected(rpmService);
        Assert.assertNotNull(connectedService.get());
        Assert.assertEquals(APP_NAME, connectedService.get().getApplicationName());
        Assert.assertSame(rpmService, connectedService.get());

    }

}

