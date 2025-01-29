/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigFactoryTest;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.agentcontrol.HealthDataProducer;
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

public class HarvestServiceTest {

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

    @Test
    public void reportingPeriod() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);
        final CountDownLatch latch = new CountDownLatch(2);
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                latch.countDown();

            }
        };
        TestHarvestService harvestService = new TestHarvestService();
        harvestService.setReportingPeriod(500L);
        harvestService.start();
        harvestService.startHarvest(rpmService);
        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
        harvestService.stop();
    }

    @Test
    public void metricLimit() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);
        final CountDownLatch latch = new CountDownLatch(2);
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                latch.countDown();
                if (latch.getCount() == 1) {
                    Assert.assertEquals(MetricIdRegistry.METRIC_LIMIT + 100, statsEngine.getSize());
                } else {
                    Assert.assertEquals(1, statsEngine.getSize());
                }

            }
        };
        TestHarvestService harvestService = new TestHarvestService();
        harvestService.setReportingPeriod(500L);
        harvestService.start();
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        for (int i = 0; i < MetricIdRegistry.METRIC_LIMIT + 100; i++) {
            Stats stats = statsEngine.getStats("Test" + String.valueOf(i));
            stats.recordDataPoint(100f);
        }
        ServiceFactory.getStatsService().doStatsWork(new MergeStatsWork("test", statsEngine), "statsWorkTest");
        harvestService.startHarvest(rpmService);
        Assert.assertTrue(latch.await(5L, TimeUnit.SECONDS));
        harvestService.stop();
    }

    @Test
    public void harvestListener() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);
        final CountDownLatch latch = new CountDownLatch(1);
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                latch.countDown();

            }
        };
        final CountDownLatch latch2 = new CountDownLatch(1);
        HarvestListener harvestListener = new HarvestListener() {
            @Override
            public void beforeHarvest(String appName, StatsEngine statsEngine) {
                latch2.countDown();
            }

            @Override
            public void afterHarvest(String appName) {
            }

        };
        TestHarvestService harvestService = new TestHarvestService();
        harvestService.setReportingPeriod(500L);
        harvestService.addHarvestListener(harvestListener);
        harvestService.start();
        harvestService.startHarvest(rpmService);
        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(latch2.await(5, TimeUnit.SECONDS));
        harvestService.stop();
    }

    @Test
    public void harvestNowWhenHarvestNotRunning() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);
        final AtomicInteger harvestCount = new AtomicInteger();
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                harvestCount.incrementAndGet();

            }
        };
        TestHarvestService harvestService = new TestHarvestService(666000L);
        harvestService.setReportingPeriod(1000000L);
        harvestService.start();
        harvestService.startHarvest(rpmService);
        // try to do an immediate harvest
        harvestService.harvestNow();
        // should run harvest
        Assert.assertEquals(1, harvestCount.get());
        harvestService.stop();
    }

    @Test
    public void multipleRPMServices() throws Exception {
        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        environment.setServerPort(null);
        final CountDownLatch latch = new CountDownLatch(2);
        MyRPMService rpmService = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                latch.countDown();

            }
        };
        final CountDownLatch latch2 = new CountDownLatch(2);
        MyRPMService rpmService2 = new MyRPMService() {
            @Override
            public void harvest(StatsEngine statsEngine) {
                latch2.countDown();

            }
        };
        TestHarvestService harvestService = new TestHarvestService();
        harvestService.setReportingPeriod(500L);
        harvestService.start();
        harvestService.startHarvest(rpmService);
        harvestService.startHarvest(rpmService2);
        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(latch2.await(5, TimeUnit.SECONDS));
        harvestService.stop();
    }

    private static class MyRPMService extends BaseRPMService {

        @Override
        public String getHostString() {
            return "hostname";
        }

        @Override
        public String getApplicationName() {
            return "test";
        }

        @Override
        public HealthDataProducer getHttpDataSenderAsHealthDataProducer() {
            return null;
        }

    }

    private static class MergeStatsWork implements StatsWork {

        private final String appName;
        private final StatsEngine statsEngine;

        private MergeStatsWork(String appName, StatsEngine statsEngine) {
            this.appName = appName;
            this.statsEngine = statsEngine;
        }

        @Override
        public void doWork(StatsEngine statsEngine) {
            statsEngine.mergeStats(this.statsEngine);
        }

        @Override
        public String getAppName() {
            return appName;
        }
    }
}
