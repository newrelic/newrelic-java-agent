/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.instrumentation.sql.NoOpTrackingSqlTracer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlowQueryAggregatorImplTest {

    private static final String APP_NAME = "Unit Test";

    private final SlowQueryAggregatorImpl aggregator = new SlowQueryAggregatorImpl();
    private SqlObfuscator sqlObfuscator;

    @After
    public void afterTest() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    private static Map<String, Object> createMap() {
        return new HashMap<>();
    }

    private Map<String, Object> createStagingMap() {
        Map<String, Object> configMap = createMap();
        configMap.put("host", "nope.example.invalid");
        configMap.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        configMap.put(AgentConfigImpl.APP_NAME, APP_NAME);
        Map<String, Object> ttconfig = createMap();
        ttconfig.put(TransactionTracerConfigImpl.ENABLED, Boolean.TRUE);
        ttconfig.put(TransactionTracerConfigImpl.COLLECT_TRACES, Boolean.TRUE);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttconfig);
        return configMap;
    }

    private MockServiceManager createServiceManager(Map<String, Object> configMap) throws Exception {
        AgentConfig config = AgentConfigFactory.createAgentConfig(configMap, null, null);

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        DatabaseService dbService = new DatabaseService();
        serviceManager.setDatabaseService(dbService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        serviceManager.setDistributedTraceService(new DistributedTraceServiceImpl());

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setEverConnected(true);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceManager.setRPMService(rpmService);

        configService.start();
        serviceManager.start();
        sqlTraceService.start();

        sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();

        serviceManager.setAttributesService(new AttributesService());

        return serviceManager;
    }

    private static final AtomicInteger count = new AtomicInteger();

    private TransactionData createTransactionData(String appName, String transactionName, long durationInMillis) {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setWebTransaction(true);
        MockDispatcherTracer rootTracer = new MockDispatcherTracer();
        rootTracer.setDurationInMilliseconds(durationInMillis);
        rootTracer.setStartTime(System.nanoTime());
        rootTracer.setEndTime(
                rootTracer.getStartTime() + TimeUnit.NANOSECONDS.convert(durationInMillis, TimeUnit.MILLISECONDS));
        TransactionTracerConfig ttconf = Mockito.mock(TransactionTracerConfig.class);
        when(ttconf.isEnabled()).thenReturn(true);
        when(ttconf.getInsertSqlMaxLength()).thenReturn(10 * 1000);

        Transaction tx = mock(Transaction.class);
        TransactionActivity txa = mock(TransactionActivity.class);
        when(txa.getTransaction()).thenReturn(tx);
        when(tx.getTransactionActivity()).thenReturn(txa);
        when(tx.getTransactionTracerConfig()).thenReturn(ttconf);
        List<SlowQueryInfo> tracers = new ArrayList<>(1);
        String sql = "select * from TABLE" + Integer.toString(count.incrementAndGet()) + " where Time = 'nigh'";
        String obfuscatedSql = sqlObfuscator.obfuscateSql(sql);
        SqlTracer sqlTracer = new NoOpTrackingSqlTracer(tx, sql);

        SlowQueryInfo slowQueryInfo = new SlowQueryInfo(null, sqlTracer, sql, obfuscatedSql, agentConfig.getSqlTraceConfig());
        tracers.add(slowQueryInfo);
        SlowQueryListener slowQueryListener = Mockito.mock(SlowQueryListener.class);
        org.mockito.Mockito.when(slowQueryListener.getSlowQueries()).thenReturn(tracers);

        return new TransactionDataTestBuilder(appName, agentConfig, rootTracer)
                .setTx(tx)
                .setRequestUri(transactionName)
                .setFrontendMetricName(transactionName)
                .setSlowQueryListener(slowQueryListener)
                .build();
    }

    @Test
    public void testSqlTracerSizeCap() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);
        int sqlInfoCount;

        List<SqlTrace> tracers;

        // Upper bound
        for (int i = 0; i < SlowQueryAggregatorImpl.MAX_SLOW_QUERY_STATEMENTS + 1; ++i) {
            TransactionData td = createTransactionData(APP_NAME, "SizeCapTest", 50);
            this.aggregator.addSlowQueriesFromTransaction(td);
        }

        sqlInfoCount = aggregator.getSlowQueryCount();
        tracers = aggregator.getAndClearSlowQueries();
        Assert.assertNotNull(tracers);
        Assert.assertEquals(SlowQueryAggregatorImpl.SLOW_QUERY_LIMIT_PER_REPORTING_PERIOD, tracers.size());
        Assert.assertEquals(SlowQueryAggregatorImpl.MAX_SLOW_QUERY_STATEMENTS, sqlInfoCount);

        // Upper bound again
        for (int i = 0; i < SlowQueryAggregatorImpl.MAX_SLOW_QUERY_STATEMENTS + 1; ++i) {
            TransactionData td = createTransactionData(APP_NAME, "SizeCapTest", 50);
            this.aggregator.addSlowQueriesFromTransaction(td);
        }

        sqlInfoCount = aggregator.getSlowQueryCount();
        tracers = aggregator.getAndClearSlowQueries();
        Assert.assertNotNull(tracers);
        Assert.assertEquals(SlowQueryAggregatorImpl.SLOW_QUERY_LIMIT_PER_REPORTING_PERIOD, tracers.size());
        Assert.assertEquals(SlowQueryAggregatorImpl.MAX_SLOW_QUERY_STATEMENTS, sqlInfoCount);
    }

    @Test
    public void testObfuscateInClause() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        Assert.assertEquals("select * from test where id in (?,?,?, ?, ?)",
                sqlObfuscator.obfuscateSql("select * from test where id in (1,2,3, 4, 5)"));
        Assert.assertEquals("select * from test where id in (?,?,?, ?, ?) and ssn in ( ?,?, ? )",
                sqlObfuscator.obfuscateSql("select * from test where id in ('a','b','c', 'd', 'e') and ssn in ( 123,456, 789 )"));
        Assert.assertEquals(
                "SELECT t0.ID, t0.TYPE FROM SVF003P.NIETVORDERBAARHEID t0 WHERE t0.FK_SCHULDENAAR_ID = ?",
                sqlObfuscator.obfuscateSql("SELECT t0.ID, t0.TYPE FROM SVF003P.NIETVORDERBAARHEID t0 WHERE t0.FK_SCHULDENAAR_ID = 77853"));
        Assert.assertEquals(
                "SELECT t0.ID, t0.TYPE FROM SVF003P.NIETVORDERBAARHEID t0 WHERE t0.FK_SCHULDENAAR_ID = ?",
                sqlObfuscator.obfuscateSql("SELECT t0.ID, t0.TYPE FROM SVF003P.NIETVORDERBAARHEID t0 WHERE t0.FK_SCHULDENAAR_ID = ?"));
    }
}
