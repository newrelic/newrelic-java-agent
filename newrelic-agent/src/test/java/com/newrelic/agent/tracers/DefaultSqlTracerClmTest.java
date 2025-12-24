/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import sql.DummyConnection;
import sql.DummyResultSet;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.newrelic.agent.AgentHelper.getFullPath;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSqlTracerClmTest {

    private String APP_NAME;

    @AfterClass
    public static void afterClass() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    @Before
    public void before() throws Exception {
        String configPath = getFullPath("/com/newrelic/agent/config/span_events_clm.yml");
        System.setProperty("newrelic.config.file", configPath);
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
        APP_NAME = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();

        AgentHelper.clearMetrics();
        Transaction.clearTransaction();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void stackTrace() throws Exception {
        DefaultSqlTracer tracer = newTracer("select * from dude");
        tracer.storeStackTrace();

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionTracerConfig ttConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);
        JSONArray json = (JSONArray) AgentHelper.serializeJSON(segment);

        Map<?,?> params = (Map<?, ?>) json.get(3);
        List<?> backtrace = (List<?>) params.get(DefaultSqlTracer.BACKTRACE_PARAMETER_NAME);

        assertTrue(backtrace.size() > 2);

        assertClmAbsent(tracer);
        tracer.finish(0, null);
        assertClm(tracer);
    }

    @SuppressWarnings("unchecked")
    public void testSqlInParameterMap() throws Exception {
        String inputSql = "select * from dudette";
        DefaultSqlTracer tracer = newTracer(inputSql);

        assertTrue(tracer.getAgentAttributes().isEmpty());

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionTracerConfig ttConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);

        assertEquals(2, tracer.getAgentAttributes().size()); // exclusive_duration_millis and sql
        assertEquals(inputSql, tracer.getAgentAttributes().get("sql"));

        JSONArray json = (JSONArray) AgentHelper.serializeJSON(segment);

        Map<?, ?> params = (Map<?, ?>) json.get(3);
        // the name changes when it is obfuscated
        String sqlStatement = (String) params.get("sql_obfuscated");

        assertEquals(inputSql, sqlStatement);

        assertClmAbsent(tracer);
        tracer.finish(0, null);
        assertClm(tracer);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHighSecurityNormalFinish() throws Exception {
        TransactionTracerConfig ttConfig = mock(TransactionTracerConfig.class);

        // These are set to -1 to allow this tracer to always go through slow sql logic. Otherwise if the tracer
        // executes in less than 1 millisecond (e.g. 0 ms rounded) it won't be greater than the threshold (0) so
        // we won't run this through the slow sql code.
        when(ttConfig.getExplainThresholdInMillis()).thenReturn(-1d);
        when(ttConfig.getExplainThresholdInNanos()).thenReturn(-1d);

        when(ttConfig.getRecordSql()).thenReturn(RecordSql.obfuscated.name());
        when(ttConfig.isExplainEnabled()).thenReturn(true);
        when(ttConfig.isEnabled()).thenReturn(true);

        AgentConfig agentConfig = AgentHelper.mockAgentConfig(ttConfig);
        when(agentConfig.isHighSecurity()).thenReturn(true);
        when(agentConfig.getTransactionTracerConfig()).thenReturn(ttConfig);

        String inputSql = "select * from dudette where ssn = 123456789";
        String obfuscatedInputSql = "select * from dudette where ssn = ?";
        DefaultSqlTracer tracer = newTracer(inputSql);

        assertTrue(tracer.getAgentAttributes().isEmpty());

        assertClmAbsent(tracer);
        tracer.finish(Opcodes.RETURN, null);

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);

        assertEquals(9, tracer.getAgentAttributes().size()); // exclusive_duration_millis, sql, sql_obfuscated, host, port_path_or_id, code.namespace, code.function, thread.id
        assertEquals(inputSql, tracer.getAgentAttributes().get("sql")); // shouldn't be obfuscated yet
        assertClm(tracer);

        JSONArray json = (JSONArray) AgentHelper.serializeJSON(segment);

        Map<?, ?> params = (Map<?, ?>) json.get(3);
        // the name changes when it is obfuscated
        assertEquals(obfuscatedInputSql, params.get("sql_obfuscated"));

        assertNull(params.get("sql")); // (raw) sql should be null since high security is on

        // Ensure that we do not send up unobfuscated sql in the intrinsics
        Transaction transaction = Transaction.getTransaction(false);
        assertNotNull(transaction);
        Map<String, Object> intrinsics = transaction.getIntrinsicAttributes();
        assertNotNull(intrinsics);
        assertNull(intrinsics.get(SqlTracer.SQL_PARAMETER_NAME));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHighSecurityErrorFinish() throws Exception {
        TransactionTracerConfig ttConfig = mock(TransactionTracerConfig.class);

        // These are set to -1 to allow this tracer to always go through slow sql logic. Otherwise if the tracer
        // executes in less than 1 millisecond (e.g. 0 ms rounded) it won't be greater than the threshold (0) so
        // we won't run this through the slow sql code.
        when(ttConfig.getExplainThresholdInMillis()).thenReturn(-1d);
        when(ttConfig.getExplainThresholdInNanos()).thenReturn(-1d);

        when(ttConfig.getRecordSql()).thenReturn(RecordSql.obfuscated.name());
        when(ttConfig.isExplainEnabled()).thenReturn(true);
        when(ttConfig.isEnabled()).thenReturn(true);

        AgentConfig agentConfig = AgentHelper.mockAgentConfig(ttConfig);
        when(agentConfig.isHighSecurity()).thenReturn(true);
        when(agentConfig.getTransactionTracerConfig()).thenReturn(ttConfig);

        String inputSql = "select * from dudette where ssn = 123456789";
        String obfuscatedInputSql = "select * from dudette where ssn = ?";
        DefaultSqlTracer tracer = newTracer(inputSql);

        assertTrue(tracer.getAgentAttributes().isEmpty());

        assertClmAbsent(tracer);
        tracer.finish(new RuntimeException());

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);

        assertEquals(6, tracer.getAgentAttributes().size()); // exclusive_duration_millis, sql, code.namespace, code.function, thread.id
        assertEquals(inputSql, (String) tracer.getAgentAttributes().get("sql")); // shouldn't be obfuscated yet
        assertClm(tracer);

        JSONArray json = (JSONArray) AgentHelper.serializeJSON(segment);

        Map params = (Map) json.get(3);
        // the name changes when it is obfuscated
        String sqlStatement = (String) params.get("sql_obfuscated");
        assertEquals(obfuscatedInputSql, sqlStatement);

        String rawSql = (String) params.get("sql");
        assertNull(rawSql); // raw sql should be null since high security is on

        // Ensure that we do not send up unobfuscated sql in the intrinsics
        Transaction transaction = Transaction.getTransaction(false);
        assertNotNull(transaction);
        Map<String, Object> intrinsics = transaction.getIntrinsicAttributes();
        assertNotNull(intrinsics);
        assertEquals(obfuscatedInputSql, intrinsics.get(SqlTracer.SQL_PARAMETER_NAME));
    }

    @Test
    public void testInsertValuesTruncates() throws Exception {
        String sql = " Insert  Into  test       \t\t   VALUES ";
        for (int i = 0; i < 100; i++) {
            sql += "(333,444,555,666,777,888,999,111,222),";
        }

        DefaultSqlTracer tracer = newTracer(sql);
        assertClmAbsent(tracer);
        tracer.finish(Opcodes.RETURN, null);

        assertEquals(getScopedMetric("test", "insert"), tracer.getMetricName());

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionTracerConfig ttConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);
        JSONArray json = (JSONArray) AgentHelper.serializeJSON(segment);

        JSONObject params = (JSONObject) json.get(3);
        String truncatedSql = (String) params.get(SqlTracer.SQL_OBFUSCATED_PARAMETER_NAME);
        assertNotNull(truncatedSql);
        assertTrue(truncatedSql.length() < sql.length());
        assertTrue(truncatedSql.endsWith("more chars)"));

        assertClm(tracer);
    }

    @Test
    public void testShowStatementGeneratesZeroDatastoreMetrics() throws Exception {
        DefaultSqlTracer tracer = newTracer("show tables");
        assertClmAbsent(tracer);
        tracer.finish(Opcodes.RETURN, null);
        assertClm(tracer);

        Set<String> data = AgentHelper.getMetrics();

        // The agent does not capture any Datastore metrics pertaining to a 'show' statement. Other metrics may potentially
        // be generated during test execution; therefore, this only asserts that no 'Datastore'-namespaced metrics were captured.
        assertThat(data, not(hasItem(containsString(DatastoreMetrics.METRIC_NAMESPACE))));
    }

    @Test
    public void testInsertSelectNoTruncation() throws Exception {
        String sql = " Insert   Into test    Select * from dude where ";
        for (int i = 0; i < 100; i++) {
            sql += "name = 'test" + i + "' or ";
        }

        DefaultSqlTracer tracer = newTracer(sql);
        assertClmAbsent(tracer);
        tracer.finish(Opcodes.RETURN, null);
        assertEquals(getScopedMetric("test", "insert"), tracer.getMetricName());
        String truncatedSql = tracer.getSql().toString();
        assertNotNull(truncatedSql);
        assertEquals(sql.length(), truncatedSql.length());
        assertFalse(truncatedSql.endsWith("more chars)"));
        assertClm(tracer);
    }

    @Test
    public void testExplain() throws SQLException {
        DefaultSqlTracer tracer = newTracer("select * from metrics");
        synchronized (this) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertClmAbsent(tracer);
        tracer.finish(Opcodes.LRETURN, new DummyResultSet()); // should return result set
        assertEquals(getScopedMetric("metrics", "select"), tracer.getMetricName());
        assertClm(tracer);
    }

    @Test
    public void testInstanceDB() throws SQLException {
        final Connection connection = mock(Connection.class);
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.associateAddress(connection, new InetSocketAddress("mycoolhost.net", 3306));
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        DefaultSqlTracer tracer = newInstanceDBTracer("select * from metrics", connection, "MySQL", "mysql");

        assertClmAbsent(tracer);
        tracer.finish(Opcodes.ARETURN, new DummyResultSet()); // should return result set
        String expectedRollupMetricName = getScopedInstanceDBMetric(DatastoreVendor.MySQL.name(), "mycoolhost.net", "3306");
        assertTrue(tracer.getRollupMetricNames().contains(expectedRollupMetricName));
        assertClm(tracer);
    }

    @Test
    public void testInstanceDBUnknown() throws SQLException {
        final Connection connection = mock(Connection.class);
        DefaultSqlTracer tracer = newInstanceDBTracer("select * from metrics", connection, "MySQL", "mysql");

        assertClmAbsent(tracer);
        tracer.finish(Opcodes.ARETURN, new DummyResultSet());
        String expectedRollupMetricName = getScopedInstanceDBMetric(DatastoreVendor.MySQL.name(), "unknown", "unknown");
        assertTrue(tracer.getRollupMetricNames().contains(expectedRollupMetricName));
        assertClm(tracer);
    }

    @Test
    public void testCacheParsedInMemoryConnections() throws SQLException {
        String url = "jdbc:h2:mem:mydbname";
        Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(mock(DatabaseMetaData.class));
        when(connection.getMetaData().getURL()).thenReturn(url);

        String dbVendor = "MySQL";
        DefaultSqlTracer sqlTracer = newInstanceDBTracer("select * from metrics", connection, dbVendor, "mysql");
        assertClmAbsent(sqlTracer);
        sqlTracer.finish(Opcodes.ARETURN, new DummyResultSet());

        assertNull(DatastoreInstanceDetection.getAddressForConnection(connection));
        String identifier = JdbcHelper.getCachedIdentifierForConnection(connection);
        assertNotNull(identifier);

        String hostname = Hostname.getHostname(ServiceFactory.getConfigService().getDefaultAgentConfig());
        String expectedInstanceMetric = getScopedInstanceDBMetric(dbVendor, hostname, "mydbname");
        assertTrue(sqlTracer.getRollupMetricNames().contains(expectedInstanceMetric));

        assertClm(sqlTracer);
    }

    @Test
    public void multipleStatements() throws SQLException {
        DefaultSqlTracer tracer = newTracer("select * from dude; select * from test");
        assertClmAbsent(tracer);
        tracer.finish(Opcodes.RETURN, null);
        assertEquals(getScopedMetric("dude", "select"), tracer.getMetricName());
        assertClm(tracer);
    }

    private DefaultSqlTracer newTracer(String sql) throws SQLException {
        DummyConnection conn = new DummyConnection();
        Statement statement = conn.createStatement();
        Transaction transaction = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Statement", "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        DefaultSqlTracer result = new DefaultSqlTracer(transaction, sig, statement, new SimpleMetricNameFormat(null),
                DefaultTracer.DEFAULT_TRACER_FLAGS);
        result.setRawSql(sql);
        AgentHelper.setLastTracer(result); // prevent "Inconsistent state! ..." errors
        return result;
    }

    private DefaultSqlTracer newInstanceDBTracer(String sql, final Connection connection, final String dbVendor, final String dbVendorName)
            throws SQLException {
        Transaction transaction = Transaction.getTransaction(true);
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Statement", "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        DefaultSqlTracer result = new OtherRootSqlTracer(transaction, sig, null, new SimpleMetricNameFormat(null),
                DefaultTracer.DEFAULT_TRACER_FLAGS, System.nanoTime());
        transaction.getTransactionActivity().tracerStarted(result);

        final JdbcDatabaseVendor jdbcDatabaseVendor = new JdbcDatabaseVendor(dbVendor, dbVendorName, true) {
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

        result.setConnectionFactory(connectionFactory);
        result.setRawSql(sql);
        result.provideConnection(connection);
        result.setConnectionFactory(connectionFactory);
        AgentHelper.setLastTracer(result); // prevent "Inconsistent state! ..." errors
        return result;
    }

    private String getScopedMetric(String table, String operation) {
        if (null == table) {
            return MessageFormat.format(DatastoreMetrics.OPERATION_METRIC, DatastoreVendor.JDBC, operation);
        } else {
            return MessageFormat.format(DatastoreMetrics.STATEMENT_METRIC, DatastoreVendor.JDBC, table, operation);
        }
    }

    private String getScopedInstanceDBMetric(String database, String host, String port) {
        String instance_metric_base = MessageFormat.format(DatastoreMetrics.INSTANCE_METRIC_BASE, database);
        String instance_id = MessageFormat.format(DatastoreMetrics.INSTANCE_ID, host, port);
        return instance_metric_base + instance_id;
    }

    @Test
    public void testSpanEventDatastore() throws SQLException {
        final Connection connection = mock(Connection.class);
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.associateAddress(connection, new InetSocketAddress("dbserver.nerd.us", 9945));
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        DefaultSqlTracer tracer = newInstanceDBTracer("SELECT price, name FROM BOOKS WHERE price <= 79.99",
                connection, "MySQL", "mysql");
        tracer.finish(Opcodes.ARETURN, new DummyResultSet());

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(tracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(1, spanEvents.size());

        SpanEvent spanEvent = Iterables.getFirst(spanEvents, null);
        assertNotNull(spanEvent);

        assertEquals("datastore", spanEvent.getIntrinsics().get("category"));
        assertEquals("MySQL", spanEvent.getAgentAttributes().get("db.system"));
        assertEquals("dbserver.nerd.us", spanEvent.getAgentAttributes().get("peer.hostname"));
        assertEquals("dbserver.nerd.us:9945", spanEvent.getAgentAttributes().get("peer.address"));
        assertEquals("SELECT price, name FROM BOOKS WHERE price <= 79.99", spanEvent.getAgentAttributes().get("db.statement"));
        assertEquals("books", spanEvent.getAgentAttributes().get("db.collection"));
        assertEquals("client", spanEvent.getIntrinsics().get("span.kind"));
        assertEquals("Datastore/statement/MySQL/books/select", spanEvent.getName());
        assertNotNull(spanEvent.getTraceId());
        assertNotNull(spanEvent.getGuid());
        assertClm(spanEvent);
    }

    @Test
    public void testSpanEventDatastoreTruncation() throws SQLException {
        final Connection connection = mock(Connection.class);
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.associateAddress(connection, new InetSocketAddress("dbserver.nerd.us", 9945));
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        String longQueryString = "SELECT price, name FROM BOOKS WHERE name = " + Strings.repeat("a", 5000);
        DefaultSqlTracer tracer = newInstanceDBTracer(longQueryString, connection, "MySQL", "mysql");
        tracer.finish(Opcodes.ARETURN, new DummyResultSet());

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(tracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(1, spanEvents.size());

        SpanEvent spanEvent = Iterables.getFirst(spanEvents, null);
        assertNotNull(spanEvent);

        assertEquals("datastore", spanEvent.getIntrinsics().get("category"));
        assertEquals("MySQL", spanEvent.getAgentAttributes().get("db.system"));
        assertEquals("dbserver.nerd.us", spanEvent.getAgentAttributes().get("peer.hostname"));
        assertEquals("dbserver.nerd.us:9945", spanEvent.getAgentAttributes().get("peer.address"));
        assertEquals(4095, spanEvent.getAgentAttributes().get("db.statement").toString().length());
        assertTrue(spanEvent.getAgentAttributes().get("db.statement").toString().endsWith("a..."));
        assertEquals("books", spanEvent.getAgentAttributes().get("db.collection"));
        assertEquals("client", spanEvent.getIntrinsics().get("span.kind"));
        assertEquals("Datastore/statement/MySQL/books/select", spanEvent.getName());
        assertNotNull(spanEvent.getTraceId());
        assertNotNull(spanEvent.getGuid());
        assertClm(spanEvent);
    }

    @Test
    public void testSpanEventDatastoreTruncationAtExactLimit() throws SQLException {
        final Connection connection = mock(Connection.class);
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.associateAddress(connection, new InetSocketAddress("dbserver.nerd.us", 9945));
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        String longQueryString = "SELECT price, name FROM BOOKS WHERE name = " + Strings.repeat("a", 1957);
        DefaultSqlTracer tracer = newInstanceDBTracer(longQueryString, connection, "MySQL", "mysql");
        tracer.finish(Opcodes.ARETURN, new DummyResultSet());

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(tracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(1, spanEvents.size());

        SpanEvent spanEvent = Iterables.getFirst(spanEvents, null);
        assertNotNull(spanEvent);

        assertEquals("datastore", spanEvent.getIntrinsics().get("category"));
        assertEquals("MySQL", spanEvent.getAgentAttributes().get("db.system"));
        assertEquals("dbserver.nerd.us", spanEvent.getAgentAttributes().get("peer.hostname"));
        assertEquals("dbserver.nerd.us", spanEvent.getAgentAttributes().get("server.address"));
        assertEquals("dbserver.nerd.us:9945", spanEvent.getAgentAttributes().get("peer.address"));
        assertEquals(2000, spanEvent.getAgentAttributes().get("db.statement").toString().length());
        assertTrue(spanEvent.getAgentAttributes().get("db.statement").toString().endsWith("aaa")); // Should not end with ... since it's exactly at the limit
        assertEquals("books", spanEvent.getAgentAttributes().get("db.collection"));
        assertEquals("client", spanEvent.getIntrinsics().get("span.kind"));
        assertEquals("Datastore/statement/MySQL/books/select", spanEvent.getName());
        assertNotNull(spanEvent.getTraceId());
        assertNotNull(spanEvent.getGuid());
        assertClm(spanEvent);
    }

    // @Test
    public void performance() throws Exception {
        String sql = "SELECT * FROM ROGER WHERE COL1 = ? AND COL2 = ? AND COL3 = ?";
        Object[] parameters = new Object[] { "String1", 1.0f, 1 };
        int count = 100000;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            DefaultSqlTracer.parameterizeSql(sql, parameters);
        }
        long stopTime = System.currentTimeMillis();
        String msg = MessageFormat.format("{0} iterations took {1} milliseconds ({2} nanoseconds per iteration)",
                count, stopTime - startTime, (stopTime - startTime) * 1000 / count);
        System.out.println(msg);
    }

    public static void assertClmAbsent(Tracer tracer) {
        assertNull(tracer.getAgentAttribute(AttributeNames.CLM_NAMESPACE));
        assertNull(tracer.getAgentAttribute(AttributeNames.CLM_FUNCTION));
    }

    public static void assertClm(Tracer tracer) {
        assertEquals("com.foo.Statement", tracer.getAgentAttribute(AttributeNames.CLM_NAMESPACE));
        assertEquals("executeQuery", tracer.getAgentAttribute(AttributeNames.CLM_FUNCTION));
    }

    public static void assertClm(SpanEvent spanEvent) {
        Map<String, Object> agentAttrs = spanEvent.getAgentAttributes();
        assertEquals("com.foo.Statement", agentAttrs.get(AttributeNames.CLM_NAMESPACE));
        assertEquals("executeQuery", agentAttrs.get(AttributeNames.CLM_FUNCTION));
    }

}
