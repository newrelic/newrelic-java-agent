/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.rpm;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigFactoryTest;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.Returns;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RPMConnectionServiceTest {

    private static TestRPMConnectionService rpmConnectionService;

    @BeforeClass
    public static void beforeClass() throws Exception {

        AgentHelper.initializeConfig();

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        AgentConfig config = AgentConfigFactory.createAgentConfig(settings, null, null);
        Environment env = new Environment(config, "c:\\test\\log");

        EnvironmentService envService = Mockito.mock(EnvironmentService.class, new Returns(env));
        serviceManager.setEnvironmentService(envService);

        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(settings);
        serviceManager.setConfigService(configService);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        rpmConnectionService = new TestRPMConnectionService();
    }

    @AfterClass
    public static void teardown() throws Exception {
        rpmConnectionService.stop();
    }

    @Test
    public void appServerPortAvailable() throws Exception {
        ServiceFactory.getEnvironmentService().getEnvironment().setServerPort(666);
        rpmConnectionService.setInitialAppServerPortDelay(0L);
        CountDownLatch latch = new CountDownLatch(1);
        MockRPMService rpmService = new MockRPMService(latch);
        rpmConnectionService.connect(rpmService);
        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void appServerPortNotAvailable() throws Exception {
        ServiceFactory.getEnvironmentService().getEnvironment().setServerPort(null);
        rpmConnectionService.setAppServerPortTimeout(0L);
        CountDownLatch latch = new CountDownLatch(1);
        MockRPMService rpmService = new MockRPMService(latch);
        rpmConnectionService.connect(rpmService);
        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void connectImmediate() throws Exception {
        ServiceFactory.getEnvironmentService().getEnvironment().setServerPort(null);
        CountDownLatch latch = new CountDownLatch(1);
        MockRPMService rpmService = new MockRPMService(latch);
        rpmConnectionService.setAppServerPortTimeout(RPMConnectionServiceImpl.APP_SERVER_PORT_TIMEOUT);
        rpmConnectionService.setInitialAppServerPortDelay(RPMConnectionServiceImpl.INITIAL_APP_SERVER_PORT_DELAY);
        rpmConnectionService.connectImmediate(rpmService);
        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void syncStartup() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "MyApplication");
        map.put(AgentConfigImpl.SYNC_STARTUP, true);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(map);
        MockServiceManager serviceManager = (MockServiceManager) ServiceFactory.getServiceManager();
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);
        CountDownLatch latch = new CountDownLatch(1);
        MockRPMService rpmService = new MockRPMService(latch);
        rpmService.setApplicationName("MyApplication");
        rpmConnectionService.connect(rpmService);
        Assert.assertEquals(0, latch.getCount());
    }

}
