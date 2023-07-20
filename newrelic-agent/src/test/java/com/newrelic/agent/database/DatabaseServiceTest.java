/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DatastoreConfigImpl;
import com.newrelic.agent.config.SqlTraceConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.database.SqlObfuscator.DefaultSqlObfuscator;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultSqlTracer;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;
import sql.DummyResultSet;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseServiceTest {

    private static final String APP_NAME = "Unit Test";

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
        configMap.put("host", "staging-collector.newrelic.com");
        configMap.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        configMap.put(AgentConfigImpl.APP_NAME, APP_NAME);
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

    @Test
    public void getDefaultSqlObfuscator() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = createMap();
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);
        createServiceManager(configMap);

        DatabaseService dbService = ServiceFactory.getDatabaseService();
        SqlObfuscator sqlObfuscator = dbService.getDefaultSqlObfuscator();
        Assert.assertEquals(sqlObfuscator.getClass(), DefaultSqlObfuscator.class);
        Assert.assertTrue(sqlObfuscator.isObfuscating());
    }

    @Test
    public void getSqlObfuscatorRaw() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = createMap();
        ttConfigMap.put("collect_traces", true);
        ttConfigMap.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.RAW_SETTING);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);
        createServiceManager(configMap);

        DatabaseService dbService = ServiceFactory.getDatabaseService();
        SqlObfuscator sqlObfuscator = dbService.getSqlObfuscator(APP_NAME);
        Assert.assertSame(SqlObfuscator.getNoObfuscationSqlObfuscator().getClass(), sqlObfuscator.getClass());
        Assert.assertFalse(sqlObfuscator.isObfuscating());
    }

    @Test
    public void getFieldSqlObfuscatorSqlFieldsRaw() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = createMap();
        ttConfigMap.put(TransactionTracerConfigImpl.OBFUSCATED_SQL_FIELDS, "credit_card, ssn");
        ttConfigMap.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.RAW_SETTING);
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);
        createServiceManager(configMap);

        DatabaseService dbService = ServiceFactory.getDatabaseService();
        SqlObfuscator sqlObfuscator = dbService.getSqlObfuscator(APP_NAME);
        Assert.assertSame(SqlObfuscator.getDefaultSqlObfuscator().getClass(), sqlObfuscator.getClass());
        Assert.assertTrue(sqlObfuscator.isObfuscating());
    }

    @Test
    public void getFieldSqlObfuscatorOff() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = createMap();
        ttConfigMap.put(TransactionTracerConfigImpl.OBFUSCATED_SQL_FIELDS, "credit_card, ssn");
        ttConfigMap.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.OFF_SETTING);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);
        createServiceManager(configMap);

        DatabaseService dbService = ServiceFactory.getDatabaseService();
        SqlObfuscator sqlObfuscator = dbService.getSqlObfuscator(APP_NAME);
        Assert.assertFalse(sqlObfuscator.isObfuscating());
        Assert.assertNull(sqlObfuscator.obfuscateSql("select * from employees where id=737366255"));
    }

    @Test
    public void getFieldSqlObfuscatorObfuscated() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = createMap();
        ttConfigMap.put(TransactionTracerConfigImpl.OBFUSCATED_SQL_FIELDS, "credit_card, ssn");
        ttConfigMap.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.OBFUSCATED_SETTING);
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);
        createServiceManager(configMap);

        DatabaseService dbService = ServiceFactory.getDatabaseService();
        SqlObfuscator sqlObfuscator = dbService.getSqlObfuscator(APP_NAME);
        Assert.assertEquals(sqlObfuscator.getClass(), SqlObfuscator.getDefaultSqlObfuscator().getClass());
        Assert.assertTrue(sqlObfuscator.isObfuscating());
    }

    @Test
    public void transactionTracerDisabled() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = createMap();
        ttConfigMap.put(TransactionTracerConfigImpl.ENABLED, false);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);
        createServiceManager(configMap);

        DatabaseService dbService = ServiceFactory.getDatabaseService();
        SqlObfuscator sqlObfuscator = dbService.getSqlObfuscator(APP_NAME);
        String sql = "select * from test";
        Assert.assertNull(sqlObfuscator.obfuscateSql(sql));
    }

    @Test
    public void configChanged() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = createMap();
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);
        MockServiceManager serviceManager = createServiceManager(configMap);
        MockConfigService configService = new MockConfigService(null);
        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(configMap, null, null);
        configService.setAgentConfig(agentConfig);
        serviceManager.setConfigService(configService);

        DatabaseService dbService = ServiceFactory.getDatabaseService();
        SqlObfuscator sqlObfuscator = dbService.getSqlObfuscator(APP_NAME);
        Assert.assertTrue(sqlObfuscator.isObfuscating());

        configMap = createStagingMap();
        ttConfigMap = createMap();
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);
        ttConfigMap.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.OFF_SETTING);
        agentConfig = AgentConfigFactory.createAgentConfig(configMap, null, null);
        configService.setAgentConfig(agentConfig);
        ((AgentConfigListener) dbService).configChanged(APP_NAME, agentConfig);

        sqlObfuscator = dbService.getSqlObfuscator(APP_NAME);
        Assert.assertFalse(sqlObfuscator.isObfuscating());
        Assert.assertNull(sqlObfuscator.obfuscateSql("select * from employees where id=737366255"));
    }

    @Test
    public void instanceDisabled() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> dbConfigMap = createMap();
        Map<String, Object> nestedMap = new HashMap<>();

        nestedMap.put(DatastoreConfigImpl.ENABLED, !DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED);
        dbConfigMap.put(DatastoreConfigImpl.INSTANCE_REPORTING, nestedMap);

        configMap.put(AgentConfigImpl.DATASTORE_TRACER, dbConfigMap);
        createServiceManager(configMap);

        final Connection connection = Mockito.mock(Connection.class);
        Transaction transaction = Transaction.getTransaction(true);
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Statement", "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        TestDefaultSqlTracer tracer = new TestDefaultSqlTracer(transaction, sig, null, new SimpleMetricNameFormat(null),
                DefaultTracer.DEFAULT_TRACER_FLAGS);

        ConnectionFactory connectionFactory = createConnectionFactory(connection);

        tracer.setConnectionFactory(connectionFactory);
        tracer.setRawSql("select * from metrics");
        tracer.provideConnection(connection);
        AgentHelper.setLastTracer(tracer);

        tracer.finish(Opcodes.ARETURN, new DummyResultSet());
        String expectedRollupMetricName = getScopedInstanceDBMetric(DatastoreVendor.MySQL.name(), "unknown", "unknown");
        Assert.assertFalse(tracer.getRolledUpMetricNamesForTesting().contains(expectedRollupMetricName));
        Transaction.clearTransaction();
    }

    @Test
    public void instanceLocalhostReplace() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> dsConfigMap = createMap();
        dsConfigMap.put(DatastoreConfigImpl.INSTANCE_REPORTING, true);
        configMap.put(AgentConfigImpl.DATASTORE_TRACER, dsConfigMap);
        createServiceManager(configMap);

        final Connection connection = Mockito.mock(Connection.class);
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.associateAddress(connection, new InetSocketAddress("localhost", 8080));
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        Transaction transaction = Transaction.getTransaction(true);
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Statement", "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        TestDefaultSqlTracer tracer = new TestDefaultSqlTracer(transaction, sig, null, new SimpleMetricNameFormat(null),
                DefaultTracer.DEFAULT_TRACER_FLAGS);

        tracer.setRawSql("select * from metrics");
        AgentHelper.setLastTracer(tracer);

        tracer.provideConnection(connection);

        ConnectionFactory connectionFactory = createConnectionFactory(connection);
        tracer.setConnectionFactory(connectionFactory);

        tracer.finish(Opcodes.ARETURN, new DummyResultSet());

        String notExpectedRollupMetricName = getScopedInstanceDBMetric(DatastoreVendor.MySQL.name(), "localhost",
                "8080");
        Assert.assertFalse(tracer.getRolledUpMetricNamesForTesting().contains(notExpectedRollupMetricName));
        String expectedRollupMetricName = getScopedInstanceDBMetric(DatastoreVendor.MySQL.name(),
                DatastoreMetrics.HOSTNAME, "8080");
        Assert.assertTrue(String.format("Expected instance metric %s found", expectedRollupMetricName),
                tracer.getRolledUpMetricNamesForTesting().contains(expectedRollupMetricName));
        Transaction.clearTransaction();
    }

    @Test
    public void instanceSlowSqlAttribute() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        Map<String, Object> ttConfigMap = createMap();
        ttConfigMap.put(TransactionTracerConfigImpl.ENABLED, true);
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        ttConfigMap.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD, 0);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttConfigMap);

        Map<String, Object> dsConfigMap = createMap();
        dsConfigMap.put(DatastoreConfigImpl.INSTANCE_REPORTING, true);
        configMap.put(AgentConfigImpl.DATASTORE_TRACER, dsConfigMap);

        Map<String, Object> stConfigMap = createMap();
        stConfigMap.put(SqlTraceConfigImpl.ENABLED, true);
        configMap.put(AgentConfigImpl.SLOW_SQL, stConfigMap);

        createServiceManager(configMap);

        final Connection connection = Mockito.mock(Connection.class);
        final String databaseName = "myDatabase";
        Mockito.when(connection.getCatalog()).thenReturn(databaseName);

        DatastoreInstanceDetection.detectConnectionAddress();
        InetSocketAddress address = new InetSocketAddress("address", 8080);
        DatastoreInstanceDetection.associateAddress(connection, address);
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        Transaction transaction = Transaction.getTransaction(true);
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Statement", "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        TestDefaultSqlTracer tracer = new TestDefaultSqlTracer(transaction, sig, null, new SimpleMetricNameFormat(null),
                DefaultTracer.DEFAULT_TRACER_FLAGS);

        tracer.setRawSql("select * from metrics");
        tracer.provideConnection(connection);
        ConnectionFactory connectionFactory = createConnectionFactory(connection);
        tracer.setConnectionFactory(connectionFactory);
        AgentHelper.setLastTracer(tracer);
        tracer.finish(Opcodes.ARETURN, new DummyResultSet());

        Assert.assertEquals(address.getHostName(), tracer.getAgentAttribute(DatastoreMetrics.DATASTORE_HOST));
        Assert.assertEquals(String.valueOf(address.getPort()), tracer.getAgentAttribute(DatastoreMetrics.DATASTORE_PORT_PATH_OR_ID));
        Assert.assertEquals(databaseName, tracer.getAgentAttribute(DatastoreMetrics.DB_INSTANCE));

        Transaction.clearTransaction();
    }

    @Test
    public void runExplainPlan_whenSqlTracerHasPlan_runExplainPlan() throws Exception {
        createServiceManager(createStagingMap());
        SqlTracer mockSqlTracer = mock(SqlTracer.class);
        ExplainPlanExecutor mockExplainPlanExecutor = mock(ExplainPlanExecutor.class);
        ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        Connection mockConnection = mock(Connection.class);
        DatabaseVendor mockDatabaseVendor = mock(DatabaseVendor.class);

        when(mockSqlTracer.getConnectionFactory()).thenReturn(mockConnectionFactory);
        when(mockSqlTracer.getExplainPlanExecutor()).thenReturn(mockExplainPlanExecutor);
        when(mockSqlTracer.hasExplainPlan()).thenReturn(false);
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnectionFactory.getDatabaseVendor()).thenReturn(mockDatabaseVendor);

        DatabaseService databaseService = new DatabaseService();
        databaseService.runExplainPlan(mockSqlTracer);

        verify(mockExplainPlanExecutor, times(1)).runExplainPlan(databaseService, mockConnection, mockDatabaseVendor);
        verify(mockConnection, times(1)).close();
    }

    @Test
    public void runExplainPlan_catchesException_isNoOp() throws Exception {
        createServiceManager(createStagingMap());
        SqlTracer mockSqlTracer = mock(SqlTracer.class);
        ExplainPlanExecutor mockExplainPlanExecutor = mock(ExplainPlanExecutor.class);
        ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        Connection mockConnection = mock(Connection.class);
        DatabaseVendor mockDatabaseVendor = mock(DatabaseVendor.class);

        when(mockSqlTracer.getConnectionFactory()).thenReturn(mockConnectionFactory);
        when(mockSqlTracer.getExplainPlanExecutor()).thenReturn(mockExplainPlanExecutor);
        when(mockSqlTracer.hasExplainPlan()).thenReturn(false);
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnectionFactory.getDatabaseVendor()).thenThrow(new RuntimeException());

        DatabaseService databaseService = new DatabaseService();
        databaseService.runExplainPlan(mockSqlTracer);

        verify(mockExplainPlanExecutor, times(0)).runExplainPlan(databaseService, mockConnection, mockDatabaseVendor);
        verify(mockConnection, times(1)).close();
    }

    private ConnectionFactory createConnectionFactory(final Connection connection) {
        final JdbcDatabaseVendor jdbcDatabaseVendor = new JdbcDatabaseVendor("MySQL", "database", true) {
            @Override
            public DatastoreVendor getDatastoreVendor() {
                return DatastoreVendor.MySQL;
            }
        };

        ConnectionFactory connectionFactory = new ConnectionFactory() {

            @Override
            public Connection getConnection() throws SQLException {
                return connection;
            }

            @Override
            public DatabaseVendor getDatabaseVendor() {
                return jdbcDatabaseVendor;
            }
        };
        return connectionFactory;
    }

    private String getScopedInstanceDBMetric(String database, String host, String port) {
        String instance_metric_base = MessageFormat.format(DatastoreMetrics.INSTANCE_METRIC_BASE, database);
        String instance_id = MessageFormat.format(DatastoreMetrics.INSTANCE_ID, host, port);
        return instance_metric_base + instance_id;
    }

    private class TestDefaultSqlTracer extends DefaultSqlTracer {

        public TestDefaultSqlTracer(Transaction transaction, ClassMethodSignature sig, Object object,
                MetricNameFormat metricNameFormatter, int tracerFlags) {
            super(transaction, sig, object, metricNameFormatter, tracerFlags);
        }

        public Set<String> getRolledUpMetricNamesForTesting() {
            return getRollupMetricNames();
        }
    }
}
