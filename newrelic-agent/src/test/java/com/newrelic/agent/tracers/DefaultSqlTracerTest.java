/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.newrelic.agent.*;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.datastore.*;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.database.DatastoreMetrics;
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
import java.util.*;

import static com.newrelic.agent.AgentHelper.getFullPath;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSqlTracerTest {

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
        String configPath = getFullPath("/com/newrelic/agent/config/span_events.yml");
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

        Map params = (Map) json.get(3);
        List backtrace = (List) params.get(DefaultSqlTracer.BACKTRACE_PARAMETER_NAME);

        assertTrue(backtrace.size() > 2);
        assertClmAbsent(tracer);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSqlInParameterMap() throws Exception {
        String inputSql = "select * from dudette";
        DefaultSqlTracer tracer = newTracer(inputSql);

        assertTrue(tracer.getAgentAttributes().isEmpty());

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionTracerConfig ttConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);

        assertEquals(2, tracer.getAgentAttributes().size()); // exclusive_duration_millis and sql
        assertEquals(inputSql, (String) tracer.getAgentAttributes().get("sql"));

        JSONArray json = (JSONArray) AgentHelper.serializeJSON(segment);

        Map params = (Map) json.get(3);
        // the name changes when it is obfuscated
        String sqlStatement = (String) params.get("sql_obfuscated");

        assertEquals(inputSql, sqlStatement);
        assertClmAbsent(tracer);
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

        tracer.finish(Opcodes.RETURN, null);

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);

        assertEquals(6, tracer.getAgentAttributes().size()); // exclusive_duration_millis, sql, sql_obfuscated, host, port_path_or_id, thread.id
        assertEquals(inputSql, (String) tracer.getAgentAttributes().get("sql")); // shouldn't be obfuscated yet

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
        assertNull(intrinsics.get(SqlTracer.SQL_PARAMETER_NAME));
        assertClmAbsent(tracer);
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

        tracer.finish(new RuntimeException());

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);

        assertEquals(3, tracer.getAgentAttributes().size()); // exclusive_duration_millis, sql, thread.id
        assertEquals(inputSql, (String) tracer.getAgentAttributes().get("sql")); // shouldn't be obfuscated yet

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
        assertClmAbsent(tracer);
    }

    @Test
    public void testTruncateSql() {
        String sql = "insert into my_table select * from dudes where id != 09349858 and something";

        assertTrue(sql == TransactionSegment.truncateSql(sql, 10000000));
        assertEquals("insert int..(65 more chars)", TransactionSegment.truncateSql(sql, 10));
    }

    @Test
    public void testInsertValuesTruncates() throws Exception {
        String sql = " Insert  Into  test       \t\t   VALUES ";
        for (int i = 0; i < 100; i++) {
            sql += "(333,444,555,666,777,888,999,111,222),";
        }

        DefaultSqlTracer tracer = newTracer(sql);
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
        assertClmAbsent(tracer);
    }

    @Test
    public void testShowStatementGeneratesZeroDatastoreMetrics() throws Exception {
        DefaultSqlTracer tracer = newTracer("show tables");
        tracer.finish(Opcodes.RETURN, null);

        Set<String> data = AgentHelper.getMetrics();

        // The agent does not capture any Datastore metrics pertaining to a 'show' statement. Other metrics may potentially
        // be generated during test execution; therefore, this only asserts that no 'Datastore'-namespaced metrics were captured.
        assertThat(data, not(hasItem(containsString(DatastoreMetrics.METRIC_NAMESPACE))));
        assertClmAbsent(tracer);
    }

    @Test
    public void testInsertSelectNoTruncation() throws Exception {
        String sql = " Insert   Into test    Select * from dude where ";
        for (int i = 0; i < 100; i++) {
            sql += "name = 'test" + i + "' or ";
        }

        DefaultSqlTracer tracer = newTracer(sql);
        tracer.finish(Opcodes.RETURN, null);
        assertEquals(getScopedMetric("test", "insert"), tracer.getMetricName());
        String truncatedSql = tracer.getSql().toString();
        assertNotNull(truncatedSql);
        assertEquals(sql.length(), truncatedSql.length());
        assertFalse(truncatedSql.endsWith("more chars)"));
        assertClmAbsent(tracer);
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

        tracer.finish(Opcodes.LRETURN, new DummyResultSet()); // should return result set
        assertEquals(getScopedMetric("metrics", "select"), tracer.getMetricName());
        assertClmAbsent(tracer);
    }

    @Test
    public void testInstanceDB() throws SQLException {
        final Connection connection = mock(Connection.class);
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.associateAddress(connection, new InetSocketAddress("mycoolhost.net", 3306));
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        DefaultSqlTracer tracer = newInstanceDBTracer("select * from metrics", connection, "MySQL", "mysql");

        tracer.finish(Opcodes.ARETURN, new DummyResultSet()); // should return result set
        String expectedRollupMetricName = getScopedInstanceDBMetric(DatastoreVendor.MySQL.name(), "mycoolhost.net", "3306");
        assertTrue(tracer.getRollupMetricNames().contains(expectedRollupMetricName));
        assertClmAbsent(tracer);
    }

    @Test
    public void testInstanceDBUnknown() throws SQLException {
        final Connection connection = mock(Connection.class);
        DefaultSqlTracer tracer = newInstanceDBTracer("select * from metrics", connection, "MySQL", "mysql");

        tracer.finish(Opcodes.ARETURN, new DummyResultSet());
        String expectedRollupMetricName = getScopedInstanceDBMetric(DatastoreVendor.MySQL.name(), "unknown", "unknown");
        assertTrue(tracer.getRollupMetricNames().contains(expectedRollupMetricName));
        assertClmAbsent(tracer);
    }

    @Test
    public void testCacheParsedInMemoryConnections() throws SQLException {
        String url = "jdbc:h2:mem:mydbname";
        Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(mock(DatabaseMetaData.class));
        when(connection.getMetaData().getURL()).thenReturn(url);

        String dbVendor = "MySQL";
        DefaultSqlTracer sqlTracer = newInstanceDBTracer("select * from metrics", connection, dbVendor, "mysql");
        sqlTracer.finish(Opcodes.ARETURN, new DummyResultSet());

        assertNull(DatastoreInstanceDetection.getAddressForConnection(connection));
        String identifier = JdbcHelper.getCachedIdentifierForConnection(connection);
        assertNotNull(identifier);

        String hostname = Hostname.getHostname(ServiceFactory.getConfigService().getDefaultAgentConfig());
        String expectedInstanceMetric = getScopedInstanceDBMetric(dbVendor, hostname, "mydbname");
        assertTrue(sqlTracer.getRollupMetricNames().contains(expectedInstanceMetric));
        assertClmAbsent(sqlTracer);
    }

    @Test
    public void multipleStatements() throws SQLException {
        DefaultSqlTracer tracer = newTracer("select * from dude; select * from test");
        tracer.finish(Opcodes.RETURN, null);
        assertEquals(getScopedMetric("dude", "select"), tracer.getMetricName());
        assertClmAbsent(tracer);
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
    public void testObfuscated() {
        String jsonString = getJSON(RecordSql.obfuscated);
        assertEquals("select * from user where ssn = ?", jsonString);
    }

    @Test
    public void testRaw() {
        String jsonString = getJSON(RecordSql.raw);
        assertEquals("select * from user where ssn = 666666666", jsonString);
    }

    private String getJSON(RecordSql recordSql) {
        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setConfigService(new MockConfigService(
                AgentConfigFactory.createAgentConfig(Collections.<String, Object>emptyMap(),
                        Collections.<String, Object>emptyMap(), null)));
        ServiceManager originalServiceManager = ServiceFactory.getServiceManager();

        try {
            ServiceFactory.setServiceManager(serviceManager);
            Transaction transaction = mock(Transaction.class);
            TransactionActivity txa = mock(TransactionActivity.class);
            when(transaction.getTransactionActivity()).thenReturn(txa);
            when(txa.getTransaction()).thenReturn(transaction);

            TransactionTracerConfig ttConfig = mock(TransactionTracerConfig.class);
            when(ttConfig.getRecordSql()).thenReturn(recordSql.toString());
            when(transaction.getTransactionTracerConfig()).thenReturn(ttConfig);

            ClassMethodSignature sig = new ClassMethodSignature("", "", "");

            DefaultSqlTracer tracer = new DefaultSqlTracer(transaction, sig, new Object(),
                    new SimpleMetricNameFormat("BoundedConcurrentCacheTest"), TracerFlags.GENERATE_SCOPED_METRIC);
            tracer.setRawSql("select * from user where ssn = ?");
            tracer.setParams(new Object[] { 666666666 });

            Object sqlObject = tracer.getSql();

            return sqlObject.toString();
        } finally {
            ServiceFactory.setServiceManager(originalServiceManager);
        }
    }

    @Test
    public void nullParameters() throws Exception {
        String sql = "SELECT * FROM ROGER WHERE COL1 = ? AND COL2 = ? AND COL3 = ? AND COL4 = ?";
        assertEquals(sql, DefaultSqlTracer.parameterizeSql(sql, null));
    }

    @Test
    public void emptyParameters() throws Exception {
        String sql = "SELECT * FROM ROGER WHERE COL1 = ? AND COL2 = ? AND COL3 = ? AND COL4 = ?";
        assertEquals(sql, DefaultSqlTracer.parameterizeSql(sql, new Object[0]));
    }

    @Test
    public void nullSql() throws Exception {
        String sql = null;
        assertEquals(sql, DefaultSqlTracer.parameterizeSql(sql, new Object[] { "String1" }));
    }

    @Test
    public void emptySql() throws Exception {
        String sql = "";
        assertEquals(sql, DefaultSqlTracer.parameterizeSql(sql, new Object[] { "String1" }));
    }

    @Test
    public void trailingParameter() throws Exception {
        String sql = "SELECT * FROM ROGER WHERE COL1 = ?";
        String expectedSql = "SELECT * FROM ROGER WHERE COL1 = 'String1'";
        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql,
                new Object[] { "String1" }));
    }

    @Test
    public void noTrailingParameter() throws Exception {
        String sql = "CALL PROC(?)";
        String expectedSql = "CALL PROC('String1')";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql,
                new Object[] { "String1" }));
    }

    @Test
    public void multipleParametersTrailing() throws Exception {
        String sql = "SELECT * FROM ROGER WHERE COL1 = ? AND COL2 = ? AND COL3 = ? AND COL4 = ?";
        String expectedSql = "SELECT * FROM ROGER WHERE COL1 = 'String1' AND COL2 = 1.0 AND COL3 = 1 AND COL4 = '2010-08-07'";
        Calendar cal = Calendar.getInstance();
        cal.set(2010, 7, 7);

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {
                "String1", 1.0f, 1, new java.sql.Date(cal.getTimeInMillis()) }));
    }

    @Test
    public void multipleParametersNoTrailing() throws Exception {
        String sql = "CALL PROC(?, ?)";
        String expectedSql = "CALL PROC('String1', 'String2')";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {
                "String1", "String2" }));
    }

    @Test
    public void literal() throws Exception {
        String sql = "select x, ? as lang from z where y = 1";
        String expectedSql = "select x, 'q' as lang from z where y = 1";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] { "q" }));
    }

    @Test
    public void allParameters() throws Exception {
        String sql = "? ? ?";
        String expectedSql = "'String1' 'String2' 'String3'";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {
                "String1", "String2", "String3" }));
    }

    @Test
    public void allParametersTrailingSpaces() throws Exception {
        String sql = "? ? ? ";
        String expectedSql = "'String1' 'String2' 'String3' ";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {
                "String1", "String2", "String3" }));
    }

    @Test
    public void missingParameters() throws Exception {
        String sql = "SELECT * FROM ROGER WHERE COL1 = ? AND COL2 = ? AND COL3 = ? AND COL4 = ?";
        String expectedSql = "SELECT * FROM ROGER WHERE COL1 = 'String1' AND COL2 = 1.0 AND COL3 = 1 AND COL4 = ?";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {
                "String1", 1.0f, 1 }));
    }

    @Test
    public void escapingQuotesParams() throws Exception {
        String sql = "UPDATE Table SET content = ? WHERE title = ?";
        String expectedSql = "UPDATE Table SET content = 'A long content ''with quotes''' WHERE title = 'Some ''Title'''";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {"A long content 'with quotes'", "Some 'Title'"}));
    }

    @Test
    public void notEscapingQuotesInQuery() throws Exception {
        String sql = "UPDATE Table SET content = 'A long content ''with quotes''' WHERE title = 'Some ''Title'''";

        assertEquals(sql, DefaultSqlTracer.parameterizeSql(sql, null));
    }

    @Test
    public void notEscapingEscapedQuoteInParam() throws Exception {
        String sql = "UPDATE Table SET content = ?";
        String expectedSql = "UPDATE Table SET content = 'Chris O\\'Dowd'";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {"Chris O\\'Dowd"}));
    }

    @Test
    public void mixedQuotes() throws Exception {
        String sql = "UPDATE Table SET content = ?";
        String expectedSql = "UPDATE Table SET content = 'Chris ''Roy'' O\\'Dowd'";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {"Chris 'Roy' O\\'Dowd"}));
    }

    @Test
    public void oracleQuotesInQuery() throws Exception {
        String sql = "UPDATE Table SET content = q'[content'with'quotes]'";

        assertEquals(sql, DefaultSqlTracer.parameterizeSql(sql, null));
    }

    @Test
    public void oracleQuotesInParam() throws Exception {
        String sql = "UPDATE Table SET content = ?";
        // since the oracle quote is inside the param, JDBC would treat it as part of a regular string.
        String expectedSql = "UPDATE Table SET content = 'q''[around''quote]'''";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {"q'[around'quote]'"}));
    }

    @Test
    public void oracleQuotesInQueryAndParam() throws Exception {
        String sql         = "UPDATE Table SET content = q'[?]'";
        // this case looks awkward, but the resulting query should be what the app gets when a PreparedStatement is used.
        String expectedSql = "UPDATE Table SET content = q'['a''b']'";

        assertEquals(expectedSql, DefaultSqlTracer.parameterizeSql(sql, new Object[] {"a'b"}));
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
        assertClmAbsent(spanEvent);
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
        assertClmAbsent(spanEvent);
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
        assertEquals("dbserver.nerd.us:9945", spanEvent.getAgentAttributes().get("peer.address"));
        assertEquals(2000, spanEvent.getAgentAttributes().get("db.statement").toString().length());
        assertTrue(spanEvent.getAgentAttributes().get("db.statement").toString().endsWith("aaa")); // Should not end with ... since it's exactly at the limit
        assertEquals("books", spanEvent.getAgentAttributes().get("db.collection"));
        assertEquals("client", spanEvent.getIntrinsics().get("span.kind"));
        assertEquals("Datastore/statement/MySQL/books/select", spanEvent.getName());
        assertNotNull(spanEvent.getTraceId());
        assertNotNull(spanEvent.getGuid());
        assertClmAbsent(spanEvent);
    }

    private static void assertClmAbsent(Tracer tracer) {
        assertNull(tracer.getAgentAttribute(AttributeNames.CLM_NAMESPACE));
        assertNull(tracer.getAgentAttribute(AttributeNames.CLM_FUNCTION));
    }

    private static void assertClmAbsent(SpanEvent spanEvent) {
        Map<String, Object> agentAttrs = spanEvent.getAgentAttributes();
        assertNull(agentAttrs.get(AttributeNames.CLM_NAMESPACE));
        assertNull(agentAttrs.get(AttributeNames.CLM_FUNCTION));
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

}
