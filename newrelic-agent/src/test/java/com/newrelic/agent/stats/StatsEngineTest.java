/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MetricData;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockNormalizer;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsEngineTest {

    /**
     * The name of the app.
     */
    private static final String APP_NAME = "Unit Test";

    public static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        map.put("apdex_t", 0.5f);
        return map;
    }

    /**
     * Creates the service manager.
     */
    @BeforeClass
    public static void beforeClass() {
        try {
            createServiceManager(createConfigMap());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failed to setup the service manager. " + e.getMessage());
        }
    }

    /**
     * Clears the transaction before each test.
     */
    @Before
    public void before() {
        Transaction.clearTransaction();
    }

    /**
     * Creates the service manager.
     *
     * @param map The configuration for the mananger.
     * @throws Exception Thrown if a problem creating the service manager.
     */
    private static void createServiceManager(Map<String, Object> map) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceManager.setRPMService(rpmService);

        configService.start();

        serviceManager.setNormalizationService(new NormalizationServiceImpl());

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);
        statsService.start();
    }

    @Test
    public void getMetricDataScopedAndUnscoped1() {
        try {
            StatsEngineImpl statsEngine = new StatsEngineImpl();

            // create the stats
            Transaction tx = Transaction.getTransaction();
            String methodName1 = "foo";
            ResponseTimeStats fooStats = tx.getTransactionActivity().getTransactionStats().getScopedStats().getOrCreateResponseTimeStats(methodName1);
            fooStats.setCallCount(1);

            String methodName2 = "hi";
            ResponseTimeStats hiStats = tx.getTransactionActivity().getTransactionStats().getScopedStats().getOrCreateResponseTimeStats(methodName2);
            hiStats.setCallCount(3);

            // add the scoped metrics under the scope
            statsEngine.mergeStatsResolvingScope(tx.getTransactionActivity().getTransactionStats(), "theScope");

            MockNormalizer metricNormalizer = new MockNormalizer();
            List<MetricData> data = statsEngine.getMetricData(metricNormalizer);
            Assert.assertEquals(4, data.size());

            // do the validation
            // foo scoped, foo unscoped, hi scoped, hi unscoped
            ResponseTimeStats[] output = new ResponseTimeStats[4];
            for (MetricData d : data) {
                String name = d.getMetricName().getName();
                if (name.equals(methodName1)) {
                    if (d.getMetricName().isScoped()) {
                        Assert.assertNull(output[0]);
                        output[0] = (ResponseTimeStats) d.getStats();
                    } else {
                        Assert.assertNull(output[1]);
                        output[1] = (ResponseTimeStats) d.getStats();
                    }
                } else {
                    Assert.assertEquals(methodName2, name);
                    if (d.getMetricName().isScoped()) {
                        Assert.assertNull(output[2]);
                        output[2] = (ResponseTimeStats) d.getStats();
                    } else {
                        Assert.assertNull(output[3]);
                        output[3] = (ResponseTimeStats) d.getStats();
                    }
                }
            }

            // validate the stats

            // they should be different objects with the same numbers
            Assert.assertFalse("The object should have the same values but be different objects", output[0] == output[1]);
            Assert.assertFalse("The object should have the same values but be different objects", output[2] == output[3]);

            // validate call count
            Assert.assertEquals(1, output[0].getCallCount());
            Assert.assertEquals(1, output[1].getCallCount());
            Assert.assertEquals(3, output[2].getCallCount());
            Assert.assertEquals(3, output[3].getCallCount());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void getMetricDataScopedAndUnscoped2() {
        try {
            StatsEngineImpl statsEngine = new StatsEngineImpl();

            // create the stats
            Transaction tx = Transaction.getTransaction();
            String methodName = "foo";
            ResponseTimeStats fooStats = tx.getTransactionActivity().getTransactionStats().getScopedStats().getOrCreateResponseTimeStats(
                    methodName);
            fooStats.recordResponseTimeInNanos(2000000000, 1000000000);

            String unscopedMetricName = "hi";
            ResponseTimeStats hiStats = tx.getTransactionActivity().getTransactionStats().getUnscopedStats().getOrCreateResponseTimeStats(
                    unscopedMetricName);
            hiStats.recordResponseTimeInNanos(500000, 400000);
            hiStats.recordResponseTimeInNanos(100000, 100000);

            // add the scoped metrics under the scope
            statsEngine.mergeStatsResolvingScope(tx.getTransactionActivity().getTransactionStats(), "theScope");

            MockNormalizer metricNormalizer = new MockNormalizer();
            List<MetricData> data = statsEngine.getMetricData(metricNormalizer);
            Assert.assertEquals(3, data.size());

            // do the validation
            // foo scoped, foo unscoped, hi unscoped
            ResponseTimeStats[] output = new ResponseTimeStats[3];
            for (MetricData d : data) {
                String name = d.getMetricName().getName();
                if (name.equals(methodName)) {
                    if (d.getMetricName().isScoped()) {
                        Assert.assertNull(output[2]);
                        output[0] = (ResponseTimeStats) d.getStats();
                    } else {
                        Assert.assertNull(output[2]);
                        output[1] = (ResponseTimeStats) d.getStats();
                    }
                } else {
                    Assert.assertEquals(unscopedMetricName, name);
                    if (d.getMetricName().isScoped()) {
                        Assert.fail("There should not be a scoped metric for " + unscopedMetricName);
                    } else {
                        Assert.assertNull(output[2]);
                        output[2] = (ResponseTimeStats) d.getStats();
                    }
                }
            }

            // validate the stats

            // they should be different objects with the same numbers
            Assert.assertFalse("The object should have the same values but be different objects", output[0] == output[1]);

            // validate variables
            Assert.assertEquals(1, output[0].getCallCount());
            Assert.assertEquals("The total was incorrect.", 2.0f, output[0].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", 1.0f, output[0].getTotalExclusiveTime(), .001f);
            Assert.assertEquals(1, output[1].getCallCount());
            Assert.assertEquals("The total was incorrect.", 2.0f, output[1].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", 1.0f, output[1].getTotalExclusiveTime(), .000001f);
            Assert.assertEquals(2, output[2].getCallCount());
            Assert.assertEquals("The total was incorrect.", .0006f, output[2].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", .0005f, output[2].getTotalExclusiveTime(), .000001f);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void getMetricDataWithJavaOther() {
        try {
            StatsEngineImpl statsEngine = new StatsEngineImpl();

            // create the stats
            Transaction tx = Transaction.getTransaction();
            String methodName1 = "foo";
            ResponseTimeStats fooStats = tx.getTransactionActivity().getTransactionStats().getScopedStats().getOrCreateResponseTimeStats(methodName1);
            fooStats.recordResponseTimeInNanos(2000000000, 1000000000);
            fooStats.recordResponseTimeInNanos(1000000000, 1000000000);
            fooStats.recordResponseTimeInNanos(2000000000, 1000000000);

            String methodName2 = "hi";
            ResponseTimeStats hiStats = tx.getTransactionActivity().getTransactionStats().getScopedStats().getOrCreateResponseTimeStats(methodName2);
            hiStats.recordResponseTimeInNanos(1, 1);

            String methodName3 = "hello";
            ResponseTimeStats helloStats = tx.getTransactionActivity().getTransactionStats().getScopedStats().getOrCreateResponseTimeStats(methodName3);
            helloStats.recordResponseTimeInNanos(2, 1);

            // add the scoped metrics under the scope
            statsEngine.mergeStatsResolvingScope(tx.getTransactionActivity().getTransactionStats(), "theScope");

            MockNormalizer metricNormalizer = new MockNormalizer();
            List<MetricData> data = statsEngine.getMetricData(metricNormalizer);
            Assert.assertEquals(4, data.size());

            // do the validation
            // foo scoped, foo unscoped, hi scoped, hi unscoped
            ResponseTimeStats[] output = new ResponseTimeStats[4];
            for (MetricData d : data) {
                String name = d.getMetricName().getName();
                if (name.equals(methodName1)) {
                    if (d.getMetricName().isScoped()) {
                        Assert.assertNull(output[0]);
                        output[0] = (ResponseTimeStats) d.getStats();
                    } else {
                        Assert.assertNull(output[1]);
                        output[1] = (ResponseTimeStats) d.getStats();
                    }
                } else {
                    // since the metrics are so small hi and hello
                    // should be combined into a java other metric
                    Assert.assertEquals("Java/other", name);
                    if (d.getMetricName().isScoped()) {
                        Assert.assertNull(output[2]);
                        output[2] = (ResponseTimeStats) d.getStats();
                    } else {
                        Assert.assertNull(output[3]);
                        output[3] = (ResponseTimeStats) d.getStats();
                    }
                }
            }

            // validate the stats

            // they should be different objects with the same numbers
            Assert.assertFalse("The object should have the same values but be different objects", output[0] == output[1]);
            Assert.assertFalse("The object should have the same values but be different objects", output[2] == output[3]);

            // validate call count
            Assert.assertEquals(3, output[0].getCallCount());
            Assert.assertEquals("The total was incorrect.", 5.0f, output[0].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", 3.0f, output[0].getTotalExclusiveTime(), .001f);
            Assert.assertEquals(3, output[1].getCallCount());
            Assert.assertEquals("The total was incorrect.", 5.0f, output[1].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", 3.0f, output[1].getTotalExclusiveTime(), .001f);
            Assert.assertEquals(2, output[2].getCallCount());
            Assert.assertEquals("The total was incorrect.", .000000003f, output[2].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", .000000002f, output[2].getTotalExclusiveTime(), .00000000001f);
            Assert.assertEquals(2, output[3].getCallCount());
            Assert.assertEquals("The total was incorrect.", .000000003f, output[3].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", .000000002f, output[3].getTotalExclusiveTime(), .00000000001f);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests that metrics with the same name but a different scope get merged into the same unscoped metric.
     */
    @Test
    public void getMetricDifferentScopes() {
        try {
            StatsEngineImpl statsEngine = new StatsEngineImpl();

            // create the stats
            Transaction tx = Transaction.getTransaction();
            String methodName1 = "foo";
            ResponseTimeStats fooStats = tx.getTransactionActivity().getTransactionStats().getScopedStats().getOrCreateResponseTimeStats(
                    methodName1);
            fooStats.recordResponseTimeInNanos(2000000000, 1000000000);
            fooStats.recordResponseTimeInNanos(1000000000, 1000000000);
            fooStats.recordResponseTimeInNanos(2000000000, 1000000000);

            fooStats.recordResponseTimeInNanos(2000000000, 1000000000);
            fooStats.recordResponseTimeInNanos(2000000000, 1000000000);
            fooStats.recordResponseTimeInNanos(2000000000, 1000000000);

            // add the scoped metrics under the scope
            statsEngine.mergeStatsResolvingScope(tx.getTransactionActivity().getTransactionStats(), "theScope");
            statsEngine.mergeStatsResolvingScope(tx.getTransactionActivity().getTransactionStats(), "secondTrans");

            MockNormalizer metricNormalizer = new MockNormalizer();
            List<MetricData> data = statsEngine.getMetricData(metricNormalizer);
            Assert.assertEquals(3, data.size());

            // do the validation
            // foo scoped, foo unscoped, hi scoped, hi unscoped
            ResponseTimeStats[] output = new ResponseTimeStats[3];
            for (MetricData d : data) {
                String name = d.getMetricName().getName();
                if (name.equals(methodName1)) {
                    if (d.getMetricName().isScoped()) {
                        if (output[0] == null) {
                            output[0] = (ResponseTimeStats) d.getStats();
                        } else {
                            Assert.assertNull(output[1]);
                            output[1] = (ResponseTimeStats) d.getStats();
                        }
                    } else {
                        Assert.assertNull(output[2]);
                        output[2] = (ResponseTimeStats) d.getStats();
                    }
                } else {
                    Assert.fail("The method name is invalid: " + name);
                }
            }

            // validate the stats

            // they should be different objects
            Assert.assertFalse("The objects should be different.", output[1] == output[2]);

            // validate call count
            Assert.assertEquals(6, output[0].getCallCount());
            Assert.assertEquals("The total was incorrect.", 11.0f, output[0].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", 6.0f, output[0].getTotalExclusiveTime(), .001f);
            Assert.assertEquals(6, output[1].getCallCount());
            Assert.assertEquals("The total was incorrect.", 11.0f, output[1].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", 6.0f, output[1].getTotalExclusiveTime(), .001f);
            Assert.assertEquals(12, output[2].getCallCount());
            Assert.assertEquals("The total was incorrect.", 22.0f, output[2].getTotal(), .001f);
            Assert.assertEquals("The total exclusive was incorrect.", 12.0f, output[2].getTotalExclusiveTime(), .000001f);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void getMetricNames() {
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        Stats stats = statsEngine.getStats("Test");
        stats.recordDataPoint(100);
        stats.recordDataPoint(200);

        List<MetricName> data = statsEngine.getMetricNames();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(1, statsEngine.getSize());
        Assert.assertEquals("Test", data.get(0).getName());
        CountStats harvestedStats = statsEngine.getStats(data.get(0));
        Assert.assertEquals(300f, harvestedStats.getTotal(), 0);
        Assert.assertEquals(2, harvestedStats.getCallCount());

        statsEngine.clear();
        data.clear();
        data = statsEngine.getMetricNames();
        Assert.assertEquals(0, data.size());
    }

    @Test
    public void getMetricNamesFromDataUsageStats() {
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        DataUsageStats stats = statsEngine.getDataUsageStats(MetricName.create("Test"));
        stats.recordDataUsage(100, 5);
        stats.recordDataUsage(300, 10);

        List<MetricName> data = statsEngine.getMetricNames();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(1, statsEngine.getSize());
        Assert.assertEquals("Test", data.get(0).getName());
        DataUsageStats harvestedStats = statsEngine.getDataUsageStats(data.get(0));
        Assert.assertEquals(400, harvestedStats.getBytesSent());
        Assert.assertEquals(15, harvestedStats.getBytesReceived());

        statsEngine.clear();
        data.clear();
        data = statsEngine.getMetricNames();
        Assert.assertEquals(0, data.size());
    }

    @Test
    public void getMetricData() {
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        Stats stats = statsEngine.getStats("Test");
        stats.recordDataPoint(100);
        stats.recordDataPoint(200);

        MockNormalizer metricNormalizer = new MockNormalizer();
        List<MetricData> data = statsEngine.getMetricData(metricNormalizer);
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(1, statsEngine.getSize());
        Assert.assertEquals("Test", data.get(0).getMetricName().getName());
        Assert.assertNull(data.get(0).getMetricId());
        Assert.assertEquals(300f, ((CountStats) data.get(0).getStats()).getTotal(), 0);
        Assert.assertEquals(2, ((CountStats) data.get(0).getStats()).getCallCount());
    }

    @Test
    public void getMetricDataFromDataUsageStats() {
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        DataUsageStats stats = statsEngine.getDataUsageStats(MetricName.create("Test"));
        stats.recordDataUsage(100, 5);
        stats.recordDataUsage(300, 10);

        MockNormalizer metricNormalizer = new MockNormalizer();
        List<MetricData> data = statsEngine.getMetricData(metricNormalizer);
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(1, statsEngine.getSize());
        Assert.assertEquals("Test", data.get(0).getMetricName().getName());
        Assert.assertNull(data.get(0).getMetricId());
        Assert.assertEquals(400, ((DataUsageStats) data.get(0).getStats()).getBytesSent());
        Assert.assertEquals(15, ((DataUsageStats) data.get(0).getStats()).getBytesReceived());
    }

    @Test
    public void getMetricDataNormalize() {
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        Stats stats = statsEngine.getStats("Test");
        stats.recordDataPoint(100);
        stats.recordDataPoint(200);

        MockNormalizer metricNormalizer = new MockNormalizer();
        Map<String, String> normalizationResults = new HashMap<>();
        normalizationResults.put("Test", "Test2");
        metricNormalizer.setNormalizationResults(normalizationResults);
        List<MetricData> data = statsEngine.getMetricData(metricNormalizer);
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(1, statsEngine.getSize());
        Assert.assertEquals("Test2", data.get(0).getMetricName().getName());
        Assert.assertNull(data.get(0).getMetricId());
        Assert.assertEquals(300f, ((CountStats) data.get(0).getStats()).getTotal(), 0);
        Assert.assertEquals(2, ((CountStats) data.get(0).getStats()).getCallCount());
    }

    @Test
    public void getMetricDataNormalizeFromDataUsageStats() {
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        DataUsageStats stats = statsEngine.getDataUsageStats(MetricName.create("Test"));
        stats.recordDataUsage(100, 5);
        stats.recordDataUsage(300, 10);

        MockNormalizer metricNormalizer = new MockNormalizer();
        Map<String, String> normalizationResults = new HashMap<>();
        normalizationResults.put("Test", "Test2");
        metricNormalizer.setNormalizationResults(normalizationResults);
        List<MetricData> data = statsEngine.getMetricData(metricNormalizer);
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(1, statsEngine.getSize());
        Assert.assertEquals("Test2", data.get(0).getMetricName().getName());
        Assert.assertNull(data.get(0).getMetricId());
        Assert.assertEquals(400, ((DataUsageStats) data.get(0).getStats()).getBytesSent());
        Assert.assertEquals(15, ((DataUsageStats) data.get(0).getStats()).getBytesReceived());
    }

    @Test
    public void merge() {
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        Stats stats1 = statsEngine.getStats("Test1");
        stats1.recordDataPoint(100f);
        Stats stats2 = statsEngine.getStats("Test2");
        stats2.recordDataPoint(100f);

        StatsEngineImpl statsEngine2 = new StatsEngineImpl();
        Stats stats3 = statsEngine2.getStats("Test3");
        stats3.recordDataPoint(100f);
        Stats stats4 = statsEngine2.getStats("Test2");
        stats4.recordDataPoint(100f);

        statsEngine.mergeStats(statsEngine2);
        Assert.assertEquals(3, statsEngine.getSize());
        Assert.assertEquals(1, stats1.getCallCount());
        Assert.assertEquals(2, stats2.getCallCount());
        Assert.assertEquals(1, stats3.getCallCount());
        Assert.assertEquals(100f, stats1.getTotal(), 0);
        Assert.assertEquals(200f, stats2.getTotal(), 0);
        Assert.assertEquals(100f, stats3.getTotal(), 0);
    }

    @Test
    public void mergeDataUsageStats() {
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        DataUsageStats stats1 = statsEngine.getDataUsageStats(MetricName.create("Test1"));
        stats1.recordDataUsage(100, 5);
        DataUsageStats stats2 = statsEngine.getDataUsageStats(MetricName.create("Test2"));
        stats2.recordDataUsage(300, 10);

        StatsEngineImpl statsEngine2 = new StatsEngineImpl();
        DataUsageStats stats3 = statsEngine.getDataUsageStats(MetricName.create("Test3"));
        stats3.recordDataUsage(200, 20);
        DataUsageStats stats4 = statsEngine.getDataUsageStats(MetricName.create("Test2"));
        stats4.recordDataUsage(400, 50);

        statsEngine.mergeStats(statsEngine2);
        Assert.assertEquals(3, statsEngine.getSize());
        Assert.assertEquals(1, stats1.getCount());
        Assert.assertEquals(2, stats2.getCount());
        Assert.assertEquals(1, stats3.getCount());
        Assert.assertEquals(100, stats1.getBytesSent());
        Assert.assertEquals(700, stats2.getBytesSent());
        Assert.assertEquals(200, stats3.getBytesSent());
        Assert.assertEquals(5, stats1.getBytesReceived());
        Assert.assertEquals(60, stats2.getBytesReceived());
        Assert.assertEquals(20, stats3.getBytesReceived());
    }

    @Test(expected=RuntimeException.class)
    public void getStats_nullMetric_shouldThrow(){
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        statsEngine.getStats((MetricName) null);
    }

    @Test(expected=RuntimeException.class)
    public void recordEmptyStats_nullMetric_shouldThrow(){
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        statsEngine.recordEmptyStats((MetricName) null);
    }

    @Test(expected=RuntimeException.class)
    public void getResponseTimeStats_nullMetric_shouldThrow(){
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        statsEngine.getResponseTimeStats((MetricName) null);
    }

    @Test(expected=RuntimeException.class)
    public void getApdexStats_nullMetric_shouldThrow(){
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        statsEngine.getApdexStats(null);
    }
    @Test(expected=RuntimeException.class)
    public void getDataUsageStats_nullMetric_shouldThrow(){
        StatsEngineImpl statsEngine = new StatsEngineImpl();
        statsEngine.getDataUsageStats(null);
    }

    @Test
    public void recordEmptyStats_addsScopedAndUnscopedMetrics_correctly(){
        MetricName scopedMetric1 = MetricName.create("buzz", "testScope1");
        MetricName scopedMetric2 = MetricName.create("fizz", "testScope2");
        MetricName unscopedMetric = MetricName.create("thud");

        StatsEngineImpl statsEngine = new StatsEngineImpl();
        Assert.assertEquals(0, statsEngine.getScopedStatsForTesting().size());
        Assert.assertEquals(0, statsEngine.getUnscopedStatsForTesting().getSize());

        statsEngine.recordEmptyStats(scopedMetric1);
        statsEngine.recordEmptyStats(scopedMetric2);
        statsEngine.recordEmptyStats(unscopedMetric);

        Assert.assertEquals(2, statsEngine.getScopedStatsForTesting().size());
        Assert.assertEquals(1, statsEngine.getUnscopedStatsForTesting().getSize());

        statsEngine.recordEmptyStats(scopedMetric1); //recording stats for the same metric should have no change.
        Assert.assertEquals(2, statsEngine.getScopedStatsForTesting().size());

        Assert.assertEquals(3, statsEngine.getSize());
        Assert.assertTrue(statsEngine.getMetricNames().contains(scopedMetric1));
        Assert.assertTrue(statsEngine.getMetricNames().contains(scopedMetric2));
        Assert.assertTrue(statsEngine.getMetricNames().contains(unscopedMetric));

    }
}
