/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class StatsServiceTest {

    private ServiceManager serviceManager;

    @Before
    public void beforeTest() throws Exception {
        serviceManager = createServiceManager(createStagingMap());
    }

    @After
    public void afterTest() throws Exception {
        serviceManager.stop();
    }

    private Map<String, Object> createStagingMap() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("host", "nope.example.invalid");
        configMap.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        configMap.put(AgentConfigImpl.APP_NAME, "MyApplication");
        return configMap;
    }

    private MockServiceManager createServiceManager(Map<String, Object> map) throws Exception {

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        StatsService statsService = new StatsServiceImpl();
        statsService.start();
        serviceManager.setStatsService(statsService);

        return serviceManager;
    }

    @Test
    public void doStatsWork() throws Exception {
        String appName = serviceManager.getConfigService().getDefaultAgentConfig().getApplicationName();
        StatsService statsService = serviceManager.getStatsService();

        // RecordMetric count 1
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        Stats stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(100f);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine), "statsWorkTest");

        // RecordMetric count 2
        statsEngine = new StatsEngineImpl();
        stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(200f);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine), "statsWorkTest");

        // RecordMetric count 3
        statsService.doStatsWork(new RecordMetric("Test1", 300f), "statsWorkTest");

        // RecordDataUsageMetric count 1
        statsEngine = new StatsEngineImpl();
        DataUsageStats dataUsageStats = statsEngine.getDataUsageStats(MetricName.create("Test2"));
        dataUsageStats.recordDataUsage(5000, 25);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine), "statsWorkTest");

        // RecordDataUsageMetric count 2
        statsEngine = new StatsEngineImpl();
        dataUsageStats = statsEngine.getDataUsageStats(MetricName.create("Test2"));
        dataUsageStats.recordDataUsage(1000, 10);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine), "statsWorkTest");

        // RecordDataUsageMetric count 3
        statsService.doStatsWork(new RecordDataUsageMetric("Test2", 100, 5), "statsWorkTest");

        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine), "statsWorkTest");
        statsEngine = new StatsEngineImpl();
        stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(200f);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine), "statsWorkTest");
        statsService.doStatsWork(new RecordMetric("Test1", 300f), "statsWorkTest");
        StatsEngine harvestStatsEngine = statsService.getStatsEngineForHarvest(appName);

        // Number of unique metrics (Test1 and Test2)
        Assert.assertEquals(2, harvestStatsEngine.getSize());

        // Test1 totals
        Assert.assertEquals(3, harvestStatsEngine.getStats("Test1").getCallCount());
        Assert.assertEquals(600f, harvestStatsEngine.getStats("Test1").getTotal(), 0);

        // Test2 totals
        Assert.assertEquals(3, harvestStatsEngine.getDataUsageStats(MetricName.create("Test2")).getCount());
        Assert.assertEquals(6100, harvestStatsEngine.getDataUsageStats(MetricName.create("Test2")).getBytesSent());
        Assert.assertEquals(40, harvestStatsEngine.getDataUsageStats(MetricName.create("Test2")).getBytesReceived());
    }

    @Test
    public void doStatsWorkMultiThreads() throws Exception {
        String appName = serviceManager.getConfigService().getDefaultAgentConfig().getApplicationName();
        final StatsService statsService = serviceManager.getStatsService();
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final CountDownLatch latch3 = new CountDownLatch(1);
        StatsEngineImpl statsEngine1 = new StatsEngineImpl();
        Stats stats = statsEngine1.getStats("Test1");
        stats.recordDataPoint(100f);
        final StatsWork statsWork1 = new MergeStatsWork(appName, statsEngine1) {
            @Override
            public void doWork(StatsEngine statsEngine) {
                latch1.countDown();
                try {
                    latch2.await();
                } catch (InterruptedException e) {
                }
                super.doWork(statsEngine);
            }

        };
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                statsService.doStatsWork(statsWork1, "statsWorkTest");
                latch3.countDown();
            }

        };
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
        latch1.await();
        StatsEngineImpl statsEngine2 = new StatsEngineImpl();
        stats = statsEngine2.getStats("Test1");
        stats.recordDataPoint(200f);
        final StatsWork statsWork2 = new MergeStatsWork(appName, statsEngine2);
        statsService.doStatsWork(statsWork2, "statsWorkTest");
        latch2.countDown();
        latch3.await();
        StatsEngine harvestStatsEngine = statsService.getStatsEngineForHarvest(appName);
        Assert.assertEquals(1, harvestStatsEngine.getSize());
        Assert.assertEquals(300f, harvestStatsEngine.getStats("Test1").getTotal(), 0);
    }

    @Test
    public void doStatsWorkAfterHarvest() throws Exception {
        String appName = serviceManager.getConfigService().getDefaultAgentConfig().getApplicationName();
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        StatsService statsService = serviceManager.getStatsService();
        Stats stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(100f);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine),"statsWorkTest" );
        StatsEngine harvestStatsEngine = statsService.getStatsEngineForHarvest(appName);
        Assert.assertEquals(1, harvestStatsEngine.getSize());
        Assert.assertEquals(100f, harvestStatsEngine.getStats("Test1").getTotal(), 0);
        statsEngine = new StatsEngineImpl();
        stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(200f);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine),"statsWorkTest");
        harvestStatsEngine = statsService.getStatsEngineForHarvest(appName);
        Assert.assertEquals(1, harvestStatsEngine.getSize());
        Assert.assertEquals(200f, harvestStatsEngine.getStats("Test1").getTotal(), 0);
    }

    @Test
    public void doStatsWorkMultiApp() throws Exception {
        StatsService statsService = serviceManager.getStatsService();
        String appName = serviceManager.getConfigService().getDefaultAgentConfig().getApplicationName();
        String appName2 = appName + "2";
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        Stats stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(100f);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine), "statsWorkTest");
        statsEngine = new StatsEngineImpl();
        stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(100f);
        statsService.doStatsWork(new MergeStatsWork(appName2, statsEngine), "statsWorkTest");
        statsEngine = new StatsEngineImpl();
        stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(200f);
        statsService.doStatsWork(new MergeStatsWork(appName, statsEngine), "statsWorkTest");
        statsEngine = new StatsEngineImpl();
        stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(200f);
        statsService.doStatsWork(new MergeStatsWork(appName2, statsEngine),"statsWorkTest");
        StatsEngine harvestStatsEngine = statsService.getStatsEngineForHarvest(appName);
        Assert.assertEquals(1, harvestStatsEngine.getSize());
        Assert.assertEquals(300f, harvestStatsEngine.getStats("Test1").getTotal(), 0);
        harvestStatsEngine = statsService.getStatsEngineForHarvest(appName2);
        Assert.assertEquals(1, harvestStatsEngine.getSize());
        Assert.assertEquals(300f, harvestStatsEngine.getStats("Test1").getTotal(), 0);
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