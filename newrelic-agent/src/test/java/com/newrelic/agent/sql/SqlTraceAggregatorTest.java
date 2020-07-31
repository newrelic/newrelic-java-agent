/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.*;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.config.*;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.*;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqlTraceAggregatorTest {
    
    private SqlObfuscator sqlObfuscator;

    @Before
    public void setup() throws Exception {
        AgentHelper.initializeConfig();

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(settings);
        serviceManager.setConfigService(configService);

        TransactionTraceService ttService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(ttService);
        serviceManager.setTransactionService(new TransactionService());
        serviceManager.setDatabaseService(new DatabaseService());
        serviceManager.setRPMServiceManager(new RPMServiceManagerImpl(AgentConnectionEstablishedListener.NOOP));
        serviceManager.setSqlTraceService(new SqlTraceServiceImpl());
        serviceManager.setStatsService(new StatsServiceImpl());
        serviceManager.setDistributedTraceService(new DistributedTraceServiceImpl());

        CircuitBreakerService circuitBreaker = new CircuitBreakerService();
        serviceManager.setCircuitBreakerService(circuitBreaker);

        sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
    }

    @Test
    public void testNotNullSlowSqlTracerInfoTransactionData() {
        SlowQueryAggregatorImpl agg = new SlowQueryAggregatorImpl();
        List<SlowQueryInfo> preInfos = createSlowSqlTracerInfos(null, -1, null);
        TransactionData data = new TransactionData(Transaction.getTransaction(), 10);
        agg.addSlowQueriesUnderLock(data, preInfos);

        List<SlowQueryInfo> actualInfos = agg.getSlowQueriesForTesting();
        Assert.assertEquals(2, actualInfos.size());

        for (SlowQueryInfo info : actualInfos) {
            Assert.assertNotNull(info.getTransactionData());
        }
    }

    @Test
    public void testInstanceInformationSlowSql() {
        List<SlowQueryInfo> slowQueryInfos = createSlowSqlTracerInfos("schenectady", 12345, "peopleDb");
        for (SlowQueryInfo slowQuery : slowQueryInfos) {
            Assert.assertEquals("schenectady", slowQuery.getParameters().get(DatastoreMetrics.DATASTORE_HOST));
            Assert.assertEquals("12345", slowQuery.getParameters().get(DatastoreMetrics.DATASTORE_PORT_PATH_OR_ID));
            Assert.assertEquals("peopleDb", slowQuery.getParameters().get(DatastoreMetrics.DB_INSTANCE));
        }

        List<SlowQueryInfo> slowSqlNoDb = createSlowSqlTracerInfos("pdx.hudson", 97213, null);
        for (SlowQueryInfo slowQuery : slowSqlNoDb) {
            Assert.assertEquals("pdx.hudson", slowQuery.getParameters().get(DatastoreMetrics.DATASTORE_HOST));
            Assert.assertEquals("97213", slowQuery.getParameters().get(DatastoreMetrics.DATASTORE_PORT_PATH_OR_ID));
            Assert.assertEquals(null, slowQuery.getParameters().get(DatastoreMetrics.DB_INSTANCE));
        }

    }

    public List<SlowQueryInfo> createSlowSqlTracerInfos(String host, int port, String databaseName) {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        SqlTraceConfig sqlTraceConfig = agentConfig.getSqlTraceConfig();
        
        List<SlowQueryInfo> infos = new ArrayList<>();
        long time = System.nanoTime();
        String sql1 = "select * from animal";
        String obfuscatedSql1 = sqlObfuscator.obfuscateSql(sql1);
        SlowQueryInfo info1 = new SlowQueryInfo(null, createTracer("animal", sql1, host, port, databaseName, time, 3),
                sql1, obfuscatedSql1, sqlTraceConfig);
        String sql2 = "select * from animal";
        String obfuscatedSql2 = sqlObfuscator.obfuscateSql(sql2);
        SlowQueryInfo info2 = new SlowQueryInfo(null, createTracer("animal", sql2, host, port, databaseName, time, 5),
                sql2, obfuscatedSql2, sqlTraceConfig);
        String sql3 = "select * from bear";
        String obfuscatedSql3 = sqlObfuscator.obfuscateSql(sql3);
        SlowQueryInfo info3 = new SlowQueryInfo(null, createTracer("bear", sql3, host, port, databaseName, time, 7),
                sql3, obfuscatedSql3, sqlTraceConfig);

        infos.add(info1);
        infos.add(info2);
        infos.add(info3);
        return infos;
    }

    public SqlTracer createTracer(final String tableName, String sql, String host, int port, String dbName,
            long time, int duration) {
        DefaultSqlTracer tracer = new OtherRootSqlTracer(Transaction.getTransaction(), new ClassMethodSignature(
                "myclazz", "mymethod", "()V"), sql, new SimpleMetricNameFormat(null),
                DefaultTracer.DEFAULT_TRACER_FLAGS, time);
        tracer.setRawSql("select * from " + tableName);
        tracer.setHost(host);
        tracer.setPort(port);
        tracer.setDatabaseName(dbName);
        Transaction.getTransaction().getTransactionActivity().tracerStarted(tracer);
        tracer.performFinishWork(time + duration, Opcodes.ALOAD, null);
        return tracer;
    }

}
