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
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultSqlTracer;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BoundedConcurrentCacheTest {
    private final int MAX_SIZE = 200;
    private BoundedConcurrentCache<String, SlowQueryInfo> cache;

    private static final AtomicInteger count = new AtomicInteger();
    private static final String APP_NAME = "My App";

    private AgentConfig agentConfig;
    private SqlObfuscator sqlObfuscator;
    private final MockDispatcherTracer rootTracer = new MockDispatcherTracer();
    private final MockDispatcher dispatcher = new MockDispatcher();
    private final String transactionName = "MyTransaction";
    private Transaction tx;
    private final SlowQueryListener slowQueryListener = Mockito.mock(SlowQueryListener.class);
    private final ClassMethodSignature cms = new ClassMethodSignature("com.systemr.access.Foo", "magicQuery", "V");

    @Before
    public void before() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);
        agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setWebTransaction(true);
        MockDispatcherTracer rootTracer = new MockDispatcherTracer();
        rootTracer.setDurationInMilliseconds(3000);
        rootTracer.setStartTime(System.nanoTime());
        rootTracer.setEndTime(rootTracer.getStartTime() + TimeUnit.NANOSECONDS.convert(3000, TimeUnit.MILLISECONDS));
        TransactionTracerConfig ttconf = Mockito.mock(TransactionTracerConfig.class);
        when(ttconf.isEnabled()).thenReturn(true);
        when(ttconf.getInsertSqlMaxLength()).thenReturn(10 * 1000);
        tx = mock(Transaction.class);
        TransactionActivity txa = mock(TransactionActivity.class);
        when(txa.getTransaction()).thenReturn(tx);
        when(tx.getTransactionActivity()).thenReturn(txa);
        when(tx.getTransactionTracerConfig()).thenReturn(ttconf);
        cache = new BoundedConcurrentCache<>(MAX_SIZE);
        sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
    }

    private Map<String, Object> createStagingMap() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("host", "nope.example.invalid");
        configMap.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        configMap.put(AgentConfigImpl.APP_NAME, APP_NAME);
        Map<String, Object> ttconfig = new HashMap<>();
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

        return serviceManager;
    }

    TransactionData createTransactionData(long startTime) {
        return new TransactionDataTestBuilder(APP_NAME, agentConfig, rootTracer)
                .setTx(tx)
                .setStartTime(startTime)
                .setDispatcher(dispatcher)
                .setRequestUri(transactionName)
                .setFrontendMetricName(transactionName)
                .setSlowQueryListener(slowQueryListener)
                .build();
    }

    private RandomSql createRandomSql() {
        return new RandomSql() {
            private String sql = null;

            @Override
            public String getSql() {
                if (sql == null) {
                    sql = generateUniqueSqlStatement();
                }

                return sql;
            }

            private final String generateUniqueSqlStatement() {
                StringBuilder sb = new StringBuilder();
                sb.append("select * from TABLE");
                sb.append(Integer.toString(count.incrementAndGet()));
                sb.append(" where Time = 'nigh'");
                String result = sb.toString();
                return result;
            }
        };
    }

    private interface RandomSql {

        String getSql();

    }

    private final String generateUniqueSqlStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("select * from TABLE");
        sb.append(Integer.toString(count.incrementAndGet()));
        sb.append(" where Time = 'nigh'");
        String result = sb.toString();
        return result;
    }

    public SqlTracer createSqlTracer(RandomSql sql) {
        SqlTracer defaultSqlTracer = new DefaultSqlTracer(tx, cms, new Object(),
                new SimpleMetricNameFormat("BoundedConcurrentCacheTest"), TracerFlags.GENERATE_SCOPED_METRIC);
        defaultSqlTracer.setRawSql(sql.getSql());
        return defaultSqlTracer;
    }

    public SqlTracer createSqlTracer(final long duration, RandomSql sql) {
        SqlTracer defaultSqlTracer = new DefaultSqlTracer(tx, cms, new Object(),
                new SimpleMetricNameFormat("BoundedConcurrentCacheTest"), TracerFlags.GENERATE_SCOPED_METRIC) {
            @Override
            public long getDuration() {
                return duration;
            }
        };
        defaultSqlTracer.setRawSql(sql.getSql());
        return defaultSqlTracer;
    }

    @Test
    public void testUpperBound() throws Exception {
        RandomSql sql = createRandomSql();
        Random random = new Random();

        for (int i = 0; i < MAX_SIZE + 50; i++) {
            sql = createRandomSql();
            String obfuscatedSql = sqlObfuscator.obfuscateSql(sql.getSql());
            SqlTracer tracer = createSqlTracer(sql);
            cache.putIfAbsent(generateUniqueSqlStatement(), new SlowQueryInfo(
                    createTransactionData(System.currentTimeMillis()), tracer, sql.getSql(), obfuscatedSql, agentConfig.getSqlTraceConfig()));
        }
        Assert.assertEquals(MAX_SIZE, cache.size());
    }

    @Test
    public void testKeepSlowerSql() {
        RandomSql sql;
        SlowQueryInfo info;
        SqlTracer tracer;
        long duration;

        // fastest sql
        sql = createRandomSql();
        String fastestSql = sql.getSql();
        String fastestObfuscatedSql = sqlObfuscator.obfuscateSql(fastestSql);
        duration = 1;
        tracer = createSqlTracer(duration, sql);
        info = new SlowQueryInfo(createTransactionData(0), tracer, fastestSql, fastestObfuscatedSql, agentConfig.getSqlTraceConfig());
        info.aggregate(createTransactionData(0), tracer);
        cache.putIfAbsent(fastestSql, info);

        // slower sql
        for (int i = 0; i < MAX_SIZE; i++) {
            duration = 10 + i;
            sql = createRandomSql();
            String obfuscatedSql = sqlObfuscator.obfuscateSql(sql.getSql());
            tracer = createSqlTracer(duration, sql);
            info = new SlowQueryInfo(createTransactionData(0), tracer, sql.getSql(), obfuscatedSql, agentConfig.getSqlTraceConfig());
            info.aggregate(createTransactionData(0), tracer);
            cache.putIfAbsent(sql.getSql(), info);
        }

        Assert.assertEquals(null, cache.get(fastestObfuscatedSql));
    }

    @Test
    public void testMultithreadedBehavior() throws Exception {
        RandomSql sql;
        SlowQueryInfo info;
        SqlTracer tracer;
        long duration;

        // fastest sql
        sql = createRandomSql();
        String fastestSql = sql.getSql();
        String fastestObfuscatedSql = sqlObfuscator.obfuscateSql(fastestSql);
        duration = 1;
        tracer = createSqlTracer(duration, sql);
        final TransactionData td = createTransactionData(0);
        info = new SlowQueryInfo(td, tracer, fastestSql, fastestObfuscatedSql, agentConfig.getSqlTraceConfig());
        info.aggregate(td, tracer);
        cache.putIfAbsent(fastestSql, info);

        // create 5 threads that each do similar work. This does its best to verify multithreaded behavior is safe
        Collection<Callable<Void>> slowSqlCallables = new ArrayList<>();
        final Object cacheClearLock = new Object();
        for (int i = 0; i < 5; i++) {
            final int current = i;
            slowSqlCallables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    // slower sql
                    for (int i = 0; i < MAX_SIZE * 2; i++) {
                        if (current == 1 && i == 50) {
                            synchronized (cacheClearLock) {
                                cache.clear();
                            }
                        }

                        long duration = 10 + i;
                        RandomSql sql = createRandomSql();
                        String obfuscatedSql = sqlObfuscator.obfuscateSql(sql.getSql());
                        SqlTracer tracer = createSqlTracer(duration, sql);
                        SlowQueryInfo info = new SlowQueryInfo(td, tracer, sql.getSql(), obfuscatedSql, agentConfig.getSqlTraceConfig());
                        info.aggregate(td, tracer);
                        synchronized (cacheClearLock) {
                            cache.putIfAbsent(sql.getSql(), info);
                            Assert.assertNotNull(cache.get(sql.getSql()));
                        }
                        cache.putReplace(sql.getSql(), info);
                        if (cache.size() == 0) {
                            cache.putReplace(sql.getSql(), info);
                        }
                        Assert.assertTrue(cache.size() > 0);
                    }
                    return null;
                }
            });
        }

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Future<Void>> futures = executorService.invokeAll(slowSqlCallables);
        for (Future<Void> future : futures) {
            future.get(10000, TimeUnit.SECONDS);
        }

        // Verify that the multithreaded nature of the test did not affect the results
        Assert.assertEquals(null, cache.get(fastestObfuscatedSql));
        Assert.assertEquals(MAX_SIZE, cache.size());
        List<SlowQueryInfo> slowQueryInfos = cache.asList();
        Assert.assertNotNull(slowQueryInfos);
        Assert.assertEquals(MAX_SIZE, slowQueryInfos.size());
        for (SlowQueryInfo slowQueryInfo : slowQueryInfos) {
            // We shouldn't find the "fastest" sql in the slow query list
            Assert.assertNotEquals(fastestObfuscatedSql, slowQueryInfo.getObfuscatedQuery());
        }
        cache.clear();
        Assert.assertEquals(0, cache.size());
        slowQueryInfos = cache.asList();
        Assert.assertNotNull(slowQueryInfos);
        Assert.assertEquals(0, slowQueryInfos.size());
    }

    @Test
    public void testPutIfAbsent() {
        RandomSql sql1 = createRandomSql();
        String obfuscatedSql1 = sqlObfuscator.obfuscateSql(sql1.getSql());
        RandomSql sql2 = createRandomSql();
        String obfuscatedSql2 = sqlObfuscator.obfuscateSql(sql2.getSql());
        SqlTracer tracer1 = createSqlTracer(2000, sql1);
        SqlTracer tracer2 = createSqlTracer(2000, sql2);

        SlowQueryInfo info1 = new SlowQueryInfo(createTransactionData(0L), tracer1, sql1.getSql(), obfuscatedSql1, agentConfig.getSqlTraceConfig());
        SlowQueryInfo info2 = new SlowQueryInfo(createTransactionData(0L), tracer1, sql1.getSql(), obfuscatedSql1, agentConfig.getSqlTraceConfig());
        SlowQueryInfo info3 = new SlowQueryInfo(createTransactionData(0L), tracer2, sql2.getSql(), obfuscatedSql2, agentConfig.getSqlTraceConfig());

        String sqlString1 = sql1.getSql();
        String sqlString2 = sql2.getSql();
        Assert.assertTrue(0 != sqlString1.compareTo(sqlString2));

        cache.putIfAbsent(sqlString1, info1);
        cache.putIfAbsent(sqlString1, info2);
        cache.putIfAbsent(sqlString2, info3);

        Assert.assertEquals(2, cache.size());
        Assert.assertTrue(cache.get(sqlString1) == info1);
        Assert.assertFalse(cache.get(sqlString1) == info2);
    }

    @Test
    public void testPutReplace() {
        RandomSql sql = createRandomSql();
        String obfuscatedSql = sqlObfuscator.obfuscateSql(sql.getSql());
        SqlTracer tracer1 = createSqlTracer(2000, sql);
        SqlTracer tracer2 = createSqlTracer(3000, sql);

        SlowQueryInfo info1 = new SlowQueryInfo(createTransactionData(0L), tracer1, sql.getSql(), obfuscatedSql, agentConfig.getSqlTraceConfig());
        SlowQueryInfo info2 = new SlowQueryInfo(createTransactionData(0L), tracer2, sql.getSql(), obfuscatedSql, agentConfig.getSqlTraceConfig());

        String sqlString = sql.getSql();
        cache.putReplace(sqlString, info1);
        cache.putReplace(sqlString, info2);
        SlowQueryInfo value = cache.get(sqlString);
        Assert.assertEquals(tracer2, value.getTracer());
        value = null;

        cache.putReplace(sqlString, info1);
        value = cache.get(sqlString);
        Assert.assertEquals(tracer1, value.getTracer());
    }
}
