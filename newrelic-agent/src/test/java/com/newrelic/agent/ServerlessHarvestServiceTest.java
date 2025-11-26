/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.agentcontrol.HealthDataProducer;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigFactoryTest;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.api.agent.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.Returns;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;

/**
 * Test suite for ServerlessHarvestService.
 * Validates that immediate harvest occurs on demand without periodic scheduling.
 */
public class ServerlessHarvestServiceTest {

    @Before
    public void setup() throws Exception {
        AgentHelper.initializeConfig();
        ConfigService configService = ConfigServiceFactory.createConfigService(mock(Logger.class), false);
        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        AgentConfig config = AgentConfigFactory.createAgentConfig(settings, null, null);
        Environment env = new Environment(config, "c:\\test\\log");

        EnvironmentService envService = Mockito.mock(EnvironmentService.class, new Returns(env));
        Mockito.doNothing().when(envService).stop();
        serviceManager.setEnvironmentService(envService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);
        statsService.start();
    }

    @After
    public void teardown() throws Exception {
        ServiceFactory.getServiceManager().stop();
        ServiceFactory.setServiceManager(null);
    }

    /**
     * Test that harvestNow() triggers an immediate harvest.
     */
    @Test
    public void testHarvestNowTriggersImmediateHarvest() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);

        final CountDownLatch harvestLatch = new CountDownLatch(1);
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                harvestLatch.countDown();
            }
        };

        ServerlessHarvestService harvestService = new ServerlessHarvestService();
        harvestService.start();
        harvestService.startHarvest(rpmService);
        harvestService.harvestNow();

        Assert.assertTrue("Harvest should be triggered immediately",
                harvestLatch.await(2, TimeUnit.SECONDS));

        harvestService.stop();
    }

    /**
     * Test that harvest listeners are invoked during serverless harvest.
     */
    @Test
    public void testHarvestListenersInvoked() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);

        final CountDownLatch beforeHarvestLatch = new CountDownLatch(1);
        final CountDownLatch afterHarvestLatch = new CountDownLatch(1);

        HarvestListener listener = new HarvestListener() {
            @Override
            public void beforeHarvest(String appName, StatsEngine statsEngine) {
                beforeHarvestLatch.countDown();
            }

            @Override
            public void afterHarvest(String appName) {
                afterHarvestLatch.countDown();
            }
        };

        MyRPMService rpmService = new MyRPMService();

        ServerlessHarvestService harvestService = new ServerlessHarvestService();
        harvestService.addHarvestListener(listener);
        harvestService.start();
        harvestService.startHarvest(rpmService);
        harvestService.harvestNow();

        Assert.assertTrue("beforeHarvest should be called",
                beforeHarvestLatch.await(2, TimeUnit.SECONDS));
        Assert.assertTrue("afterHarvest should be called",
                afterHarvestLatch.await(2, TimeUnit.SECONDS));

        harvestService.stop();
    }

    /**
     * Test that multiple harvest calls work correctly.
     */
    @Test
    public void testMultipleHarvestCalls() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);

        final AtomicInteger harvestCount = new AtomicInteger(0);
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                harvestCount.incrementAndGet();
            }
        };

        ServerlessHarvestService harvestService = new ServerlessHarvestService();
        harvestService.start();
        harvestService.startHarvest(rpmService);

        // Trigger multiple harvests (synchronous in serverless mode)
        harvestService.harvestNow();
        harvestService.harvestNow();
        harvestService.harvestNow();

        Assert.assertEquals("All three harvests should be executed", 3, harvestCount.get());

        harvestService.stop();
    }


    /**
     * Test that multiple RPM services are supported.
     */
    @Test
    public void testMultipleRPMServices() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);

        final CountDownLatch service1Latch = new CountDownLatch(1);
        final CountDownLatch service2Latch = new CountDownLatch(1);

        MyRPMService rpmService1 = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                service1Latch.countDown();
            }

            @Override
            public String getApplicationName() {
                return "test1";
            }
        };

        MyRPMService rpmService2 = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                service2Latch.countDown();
            }

            @Override
            public String getApplicationName() {
                return "test2";
            }
        };

        ServerlessHarvestService harvestService = new ServerlessHarvestService();
        harvestService.start();
        harvestService.startHarvest(rpmService1);
        harvestService.startHarvest(rpmService2);
        harvestService.harvestNow();

        Assert.assertTrue("First RPM service should be harvested",
                service1Latch.await(2, TimeUnit.SECONDS));
        Assert.assertTrue("Second RPM service should be harvested",
                service2Latch.await(2, TimeUnit.SECONDS));

        harvestService.stop();
    }

    /**
     * Test that harvest is skipped when RPM service is not connected.
     */
    @Test
    public void testHarvestSkippedWhenNotConnected() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);

        final AtomicInteger harvestCount = new AtomicInteger(0);
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                harvestCount.incrementAndGet();
            }

            @Override
            public boolean isConnected() {
                return false; // Not connected
            }
        };

        ServerlessHarvestService harvestService = new ServerlessHarvestService();
        harvestService.start();
        harvestService.startHarvest(rpmService);

        harvestService.harvestNow();

        Assert.assertEquals("Harvest should be skipped when not connected", 0, harvestCount.get());

        harvestService.stop();
    }

    /**
     * Test that no periodic scheduling occurs (harvest should only happen on demand).
     */
    @Test
    public void testNoPeriodicScheduling() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);

        final AtomicInteger harvestCount = new AtomicInteger(0);
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                harvestCount.incrementAndGet();
            }
        };

        ServerlessHarvestService harvestService = new ServerlessHarvestService();
        harvestService.start();
        harvestService.startHarvest(rpmService);

        Thread.sleep(1500);

        Assert.assertEquals("No automatic harvests should occur in serverless mode",
                0, harvestCount.get());

        harvestService.stop();
    }


    private static class MyRPMService extends BaseRPMService {

        @Override
        public HealthDataProducer getHttpDataSenderAsHealthDataProducer() {
            return null;
        }
    }
}
