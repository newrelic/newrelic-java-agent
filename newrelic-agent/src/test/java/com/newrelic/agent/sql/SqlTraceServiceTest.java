/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.ConnectionConfigListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.UnknownDatabaseVendor;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootSqlTracer;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.api.agent.ApplicationNamePriority;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;
import sql.DummyConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.InflaterInputStream;

public class SqlTraceServiceTest {

    private static final String APP_NAME = "Unit Test";

    @After
    public void afterTest() throws Exception {
        Transaction.clearTransaction();
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
        Map<String, Object> ttMap = createMap();
        ttMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return configMap;
    }

    private MockServiceManager createServiceManager(Map<String, Object> configMap) throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ClassTransformerService classTransformerService = Mockito.mock(ClassTransformerService.class);
        serviceManager.setClassTransformerService(classTransformerService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        DatabaseService dbService = new DatabaseService();
        serviceManager.setDatabaseService(dbService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        serviceManager.setAttributesService(new AttributesService());

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

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

    private long getExplainThresholdInMillis() {
        double explainThresholdInMillis = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig().getExplainThresholdInMillis();
        return (long) explainThresholdInMillis;
    }

    @Test
    public void isEnabled() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        SqlTraceService sqlTraceService = ServiceFactory.getSqlTraceService();
        Assert.assertTrue(sqlTraceService.isEnabled());
    }

    @Test
    public void overExplainThreshold() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(2, sqlTraces.size());

        long expectedDuration = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
        String expectedSql = "select * from dude where somevalue = ?";
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        long expectedId = sqlObfuscator.obfuscateSql(expectedSql).hashCode();
        SqlTrace sqlTrace = getSqlTrace(expectedId, sqlTraces);
        Assert.assertNotNull(sqlTrace);
        Assert.assertEquals(expectedId, sqlTrace.getId());
        Assert.assertEquals(expectedSql, sqlTrace.getQuery());
        Assert.assertEquals(2, sqlTrace.getCallCount());
        Assert.assertEquals(expectedDuration * 2, sqlTrace.getTotal());
        Assert.assertEquals(expectedDuration, sqlTrace.getMin());
        Assert.assertEquals(expectedDuration, sqlTrace.getMax());

        expectedSql = "select * from dudette where somevalue = ?";
        expectedId = sqlObfuscator.obfuscateSql(expectedSql).hashCode();
        sqlTrace = getSqlTrace(expectedId, sqlTraces);
        Assert.assertNotNull(sqlTrace);
        Assert.assertEquals(expectedId, sqlTrace.getId());
        Assert.assertEquals(expectedSql, sqlTrace.getQuery());
        Assert.assertEquals(1, sqlTrace.getCallCount());
        Assert.assertEquals(expectedDuration, sqlTrace.getTotal());
        Assert.assertEquals(expectedDuration, sqlTrace.getMin());
        Assert.assertEquals(expectedDuration, sqlTrace.getMax());
    }

    @Test
    public void notOverExplainThreshold() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis(), TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(0, sqlTraces.size());

    }

    @Test
    public void recordSqlOff() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = new HashMap<>();
        configMap.put("transaction_tracer", ttConfigMap);
        ttConfigMap.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.OFF_SETTING);
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(0, sqlTraces.size());

    }

    @Test
    public void transactionTracerNotEnabled() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = new HashMap<>();
        configMap.put("transaction_tracer", ttConfigMap);
        ttConfigMap.put("enabled", false);
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(0, sqlTraces.size());

    }

    @Test
    public void serverConfigChanges() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> sqlMap = createMap();
        sqlMap.put("enabled", true);
        configMap.put(AgentConfigImpl.SLOW_SQL, sqlMap);
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(2, sqlTraces.size());

        Map<String, Object> serverData = createMap();
        Map<String, Object> agentMap = createMap();
        serverData.put(AgentConfigFactory.AGENT_CONFIG, agentMap);
        agentMap.put(AgentConfigFactory.SLOW_SQL_PREFIX + "enabled", false);
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        ConnectionConfigListener connectionConfigListener = rpmServiceManager.getConnectionConfigListener();
        connectionConfigListener.connected(mockRPMService, serverData);

        // run a transaction
        requestDispatcherTracer = startDispatcherTracer();
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(0, sqlTraces.size());

        serverData = createMap();
        agentMap = createMap();
        serverData.put(AgentConfigFactory.AGENT_CONFIG, agentMap);
        agentMap.put(AgentConfigFactory.SLOW_SQL_PREFIX + "enabled", true);
        connectionConfigListener.connected(mockRPMService, serverData);

        // run a transaction
        requestDispatcherTracer = startDispatcherTracer();
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(2, sqlTraces.size());

    }

    @Test
    public void multipleApps() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        // run a transaction in "Test" app
        Tracer requestDispatcherTracer = startDispatcherTracer("Test");
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        Transaction.getTransaction().setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE, "Test");
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a transaction in "Test2" app
        requestDispatcherTracer = startDispatcherTracer("Test2");
        startSqlTracer("select * from dude2 where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude2 where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette2 where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        Transaction.getTransaction().setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE, "Test2");
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest for "Test"
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest("Test", new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMServiceManager()
                .getOrCreateRPMService("Test");
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(2, sqlTraces.size());

        // run a harvest for "Test2"
        mockharvestService.runHarvest("Test2", new StatsEngineImpl());
        mockRPMService = (MockRPMService) ServiceFactory.getRPMServiceManager()
                .getOrCreateRPMService("Test2");
        sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(2, sqlTraces.size());

    }

    @Test
    public void multipleHarvests() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        SqlTraceService sqlTraceService = ServiceFactory.getSqlTraceService();
        Assert.assertTrue(sqlTraceService.isEnabled());

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(2, sqlTraces.size());

        // run a transaction
        requestDispatcherTracer = startDispatcherTracer();
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(2, sqlTraces.size());

        long expectedDuration = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
        String expectedSql = "select * from dude where somevalue = ?";
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        long expectedId = sqlObfuscator.obfuscateSql(expectedSql).hashCode();
        SqlTrace sqlTrace = getSqlTrace(expectedId, sqlTraces);
        Assert.assertNotNull(sqlTrace);
        Assert.assertEquals(expectedId, sqlTrace.getId());
        Assert.assertEquals(expectedSql, sqlTrace.getQuery());
        Assert.assertEquals(2, sqlTrace.getCallCount());
        Assert.assertEquals(expectedDuration * 2, sqlTrace.getTotal());
        Assert.assertEquals(expectedDuration, sqlTrace.getMin());
        Assert.assertEquals(expectedDuration, sqlTrace.getMax());

        expectedSql = "select * from dudette where somevalue = ?";
        expectedId = sqlObfuscator.obfuscateSql(expectedSql).hashCode();
        sqlTrace = getSqlTrace(expectedId, sqlTraces);
        Assert.assertNotNull(sqlTrace);
        Assert.assertEquals(expectedId, sqlTrace.getId());
        Assert.assertEquals(expectedSql, sqlTrace.getQuery());
        Assert.assertEquals(1, sqlTrace.getCallCount());
        Assert.assertEquals(expectedDuration, sqlTrace.getTotal());
        Assert.assertEquals(expectedDuration, sqlTrace.getMin());
        Assert.assertEquals(expectedDuration, sqlTrace.getMax());
    }

    @Test
    public void overSqlLimit() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        int sqlLimit = SlowQueryAggregatorImpl.SLOW_QUERY_LIMIT_PER_REPORTING_PERIOD;

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        for (int i = 0; i < sqlLimit * 2; i++) {
            String sql = "select * from dude where column" + i + " = 'cool'";
            long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1 + i, TimeUnit.MILLISECONDS);
            startSqlTracer(sql, duration).finish(Opcodes.RETURN, null);
        }
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(sqlLimit, sqlTraces.size());

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        for (int i = sqlLimit; i < sqlLimit * 2; i++) {
            String expectedSql = "select * from dude where column" + i  + " = ?";
            long expectedId = sqlObfuscator.obfuscateSql(expectedSql).hashCode();
            SqlTrace sqlTrace = getSqlTrace(expectedId, sqlTraces);
            Assert.assertNotNull(sqlTrace);
            long expectedDuration = getExplainThresholdInMillis() + 1 + i;
            Assert.assertEquals(expectedId, sqlTrace.getId());
            Assert.assertEquals(expectedSql, sqlTrace.getQuery());
            Assert.assertEquals(1, sqlTrace.getCallCount());
            Assert.assertEquals(expectedDuration, sqlTrace.getTotal());
            Assert.assertEquals(expectedDuration, sqlTrace.getMin());
            Assert.assertEquals(expectedDuration, sqlTrace.getMax());
        }

    }

    @Test
    public void multipleTransactions() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run another transaction
        requestDispatcherTracer = startDispatcherTracer();
        long duration2 = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration2).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration2).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from dudette where somevalue = 'cool'", duration2).finish(Opcodes.RETURN, null);
        startSqlTracer("select * from duderino where somevalue = 'cool'", duration2).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        // verify results
        Assert.assertEquals(3, sqlTraces.size());

        long expectedDuration = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
        long expectedDuration2 = TimeUnit.MILLISECONDS.convert(duration2, TimeUnit.NANOSECONDS);
        String expectedSql = "select * from dude where somevalue = ?";
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        long expectedId = sqlObfuscator.obfuscateSql(expectedSql).hashCode();
        SqlTrace sqlTrace = getSqlTrace(expectedId, sqlTraces);
        Assert.assertNotNull(sqlTrace);
        Assert.assertEquals(expectedId, sqlTrace.getId());
        Assert.assertEquals(expectedSql, sqlTrace.getQuery());
        Assert.assertEquals(4, sqlTrace.getCallCount());
        Assert.assertEquals((expectedDuration * 2) + (expectedDuration2 * 2), sqlTrace.getTotal());
        Assert.assertEquals(expectedDuration, sqlTrace.getMin());
        Assert.assertEquals(expectedDuration2, sqlTrace.getMax());

        expectedSql = "select * from dudette where somevalue = ?";
        expectedId = sqlObfuscator.obfuscateSql(expectedSql).hashCode();
        sqlTrace = getSqlTrace(expectedId, sqlTraces);
        Assert.assertNotNull(sqlTrace);
        Assert.assertEquals(expectedId, sqlTrace.getId());
        Assert.assertEquals(expectedSql, sqlTrace.getQuery());
        Assert.assertEquals(2, sqlTrace.getCallCount());
        Assert.assertEquals(expectedDuration + expectedDuration2, sqlTrace.getTotal());
        Assert.assertEquals(expectedDuration, sqlTrace.getMin());
        Assert.assertEquals(expectedDuration2, sqlTrace.getMax());

        expectedSql = "select * from duderino where somevalue = ?";
        expectedId = sqlObfuscator.obfuscateSql(expectedSql).hashCode();
        sqlTrace = getSqlTrace(expectedId, sqlTraces);
        Assert.assertNotNull(sqlTrace);
        Assert.assertEquals(expectedId, sqlTrace.getId());
        Assert.assertEquals(expectedSql, sqlTrace.getQuery());
        Assert.assertEquals(1, sqlTrace.getCallCount());
        Assert.assertEquals(expectedDuration2, sqlTrace.getTotal());
        Assert.assertEquals(expectedDuration2, sqlTrace.getMin());
        Assert.assertEquals(expectedDuration2, sqlTrace.getMax());
    }

    @Test
    public void insertSqlMaxLength() throws Exception {

        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        char[] charData = new char[2100];
        String sql = "select * from dude where  where somevalue = 'cool'";
        for (int i = 0; i < sql.length(); i++) {
            charData[i] = sql.charAt(i);
        }
        for (int i = sql.length(); i < charData.length; i++) {
            charData[i] = 'a';
        }
        sql = String.valueOf(charData);
        startSqlTracer(sql, duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        Assert.assertEquals(1, sqlTraces.size());
        SqlTrace sqlTrace = sqlTraces.get(0);
        int maxSqlLength = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig().getInsertSqlMaxLength();
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        sql = sqlObfuscator.obfuscateSql(sql);
        Assert.assertEquals(TransactionSegment.truncateSql(sql, maxSqlLength).length(), sqlTrace.getQuery().length());
    }

    @Test
    public void serializeSqlTrace() throws Exception {

        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        Assert.assertEquals(1, sqlTraces.size());
        SqlTrace sqlTrace = sqlTraces.get(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(os);
        JSONValue.writeJSONString(sqlTraces, writer);
        writer.close();
        String json = os.toString();
        JSONParser parser = new JSONParser();
        JSONArray deserializedSqlTraces = (JSONArray) parser.parse(json);
        Assert.assertEquals(1, deserializedSqlTraces.size());
        JSONArray deserializedSqlTrace = (JSONArray) deserializedSqlTraces.get(0);
        Assert.assertEquals(10, deserializedSqlTrace.size());
        Assert.assertEquals(sqlTrace.getBlameMetricName(), deserializedSqlTrace.get(0));
        Assert.assertEquals(sqlTrace.getUri(), deserializedSqlTrace.get(1));
        Assert.assertEquals(sqlTrace.getId(), deserializedSqlTrace.get(2));
        Assert.assertEquals(sqlTrace.getQuery(), deserializedSqlTrace.get(3));
        Assert.assertEquals(sqlTrace.getMetricName(), deserializedSqlTrace.get(4));
        Assert.assertEquals(Integer.valueOf(sqlTrace.getCallCount()).longValue(),
                deserializedSqlTrace.get(5));
        Assert.assertEquals(sqlTrace.getTotal(), deserializedSqlTrace.get(6));
        Assert.assertEquals(sqlTrace.getMin(), deserializedSqlTrace.get(7));
        Assert.assertEquals(sqlTrace.getMax(), deserializedSqlTrace.get(8));
        Map<String, Object> decodedParams = (Map<String, Object>) decodeParams(deserializedSqlTrace.get(9));
        Map<String, Object> sqlTraceParams = sqlTrace.getParameters();

        Double decodedPriority = (Double) decodedParams.remove("priority");
        Float sqlTracePriority = (Float) sqlTraceParams.remove("priority");

        Assert.assertEquals(decodedParams, sqlTraceParams);

        Assert.assertEquals(decodedPriority, sqlTracePriority, 0.0000001f);
    }

    @Test
    public void serializeSqlTraceSimpleCompression() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        configMap.put(AgentConfigImpl.SIMPLE_COMPRESSION_PROPERTY, true);
        createServiceManager(configMap);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer();
        long duration = TimeUnit.NANOSECONDS.convert(getExplainThresholdInMillis() + 1, TimeUnit.MILLISECONDS);
        startSqlTracer("select * from dude where somevalue = 'cool'", duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        // run a harvest
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        MockRPMService mockRPMService = (MockRPMService) ServiceFactory.getRPMService();
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();

        Assert.assertEquals(1, sqlTraces.size());
        SqlTrace sqlTrace = sqlTraces.get(0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(os);
        JSONValue.writeJSONString(sqlTraces, writer);
        writer.close();
        String json = os.toString();
        JSONParser parser = new JSONParser();
        JSONArray deserializedSqlTraces = (JSONArray) parser.parse(json);
        Assert.assertEquals(1, deserializedSqlTraces.size());
        JSONArray deserializedSqlTrace = (JSONArray) deserializedSqlTraces.get(0);
        Assert.assertEquals(10, deserializedSqlTrace.size());
        Assert.assertEquals(sqlTrace.getBlameMetricName(), deserializedSqlTrace.get(0));
        Assert.assertEquals(sqlTrace.getUri(), deserializedSqlTrace.get(1));
        Assert.assertEquals(sqlTrace.getId(), deserializedSqlTrace.get(2));
        Assert.assertEquals(sqlTrace.getQuery(), deserializedSqlTrace.get(3));
        Assert.assertEquals(sqlTrace.getMetricName(), deserializedSqlTrace.get(4));
        Assert.assertEquals(Integer.valueOf(sqlTrace.getCallCount()).longValue(),
                deserializedSqlTrace.get(5));
        Assert.assertEquals(sqlTrace.getTotal(), deserializedSqlTrace.get(6));
        Assert.assertEquals(sqlTrace.getMin(), deserializedSqlTrace.get(7));
        Assert.assertEquals(sqlTrace.getMax(), deserializedSqlTrace.get(8));
        Map<String, Object> decodedParams = (Map<String, Object>) decodeSimpleCompressionParams(deserializedSqlTrace.get(9));
        Map<String, Object> sqlTraceParams = sqlTrace.getParameters();

        Double decodedPriority = (Double) decodedParams.remove("priority");
        Float sqlTracePriority = (Float) sqlTraceParams.remove("priority");

        Assert.assertEquals(decodedParams, sqlTraceParams);

        Assert.assertEquals(decodedPriority, sqlTracePriority, 0.0000001f);
    }

    private Object decodeParams(Object object) {
        byte[] bytes = Base64.getDecoder().decode(object.toString());
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        InputStream zipStream = new InflaterInputStream(inputStream);
        Reader in = new InputStreamReader(zipStream);
        return JSONValue.parse(in);
    }

    private Object decodeSimpleCompressionParams(Object object) {
        return JSONValue.parse(object.toString());
    }

    private SqlTracer startSqlTracer(final String sql, final long duration) throws SQLException {
        DummyConnection conn = new DummyConnection();
        Statement statement = conn.createStatement();
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Statement", "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        SqlTracer sqlTracer = new OtherRootSqlTracer(tx, sig, statement, new SimpleMetricNameFormat(null)) {
            @Override
            public long getDuration() {
                return duration;
            }

            @Override
            public Object getSql() {
                return sql;
            }
        };
        sqlTracer.setConnectionFactory(new ConnectionFactory() {
            @Override
            public Connection getConnection() throws SQLException {
                return null;
            }

            @Override
            public DatabaseVendor getDatabaseVendor() {
                return UnknownDatabaseVendor.INSTANCE;
            }
        });
        sqlTracer.setRawSql(sql);
        tx.getTransactionActivity().tracerStarted(sqlTracer);
        return sqlTracer;
    }

    private Tracer startDispatcherTracer() throws Exception {
        return startDispatcherTracer(APP_NAME);
    }

    private Tracer startDispatcherTracer(String appName) throws Exception {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        Tracer requestDispatcherTracer = new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
        tx.getTransactionActivity().tracerStarted(requestDispatcherTracer);
        tx.setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE, appName);
        return requestDispatcherTracer;
    }

    private SqlTrace getSqlTrace(long id, List<SqlTrace> sqlTraces) {
        for (SqlTrace sqlTrace : sqlTraces) {
            if (id == sqlTrace.getId()) {
                return sqlTrace;
            }
        }
        return null;
    }

}
