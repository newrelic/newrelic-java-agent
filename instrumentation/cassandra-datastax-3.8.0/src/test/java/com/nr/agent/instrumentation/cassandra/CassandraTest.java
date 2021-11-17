/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.QueryTrace;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.google.common.collect.Iterables;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java7IncompatibleTest;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/*
 * Gradle hangs indefinitely when these tests run on Java 16
 *
 * FIXME capture jmap/jstack thread dumps?
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.datastax.driver.core")
@Category({ Java7IncompatibleTest.class, Java16IncompatibleTest.class, Java17IncompatibleTest.class})
public class CassandraTest {

    private static final String CASSANDRA_PRODUCT = DatastoreVendor.Cassandra.toString();
    private Cluster cluster;
    private Session session;

    private String hostName;
    private int port;
    private String databaseName;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Embedded Cassandra doesn't play nice in java9 - when you attempt to start it, it attempts to create and initialize
        // a directory on the local file system. It uses its own FileUtils class do so, which contains a static
        // initialization block that tries to cast a ByteBuffer to a DirectBuffer, which doesn't exist in Java 9. This falls
        // through to a catch block, which subsequently calls JVMStabilityInspector.inspectThrowable(t), which in turn
        // calls DatabaseDescriptor.getDiskFailurePolicy(), and that, in turn, relies on the directory having been created.
        URL config = CassandraTest.class.getResource("/cu-cassandra.yaml");
        System.setProperty("cassandra.config", config.toString());
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
    }

    @Before
    public void before() throws Exception {
        port = EmbeddedCassandraServerHelper.getNativeTransportPort();
        hostName = DatastoreMetrics.replaceLocalhost(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port).getHostName());
        databaseName = "demo";

        cluster = Cluster.builder().withPort(port).addContactPoint("127.0.0.1").build();
        session = cluster.connect();

        session.execute("DROP KEYSPACE IF EXISTS " + databaseName + ";");
        session.execute(
                "CREATE KEYSPACE " + databaseName + " WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
        session.execute("USE " + databaseName + ";");

        session.execute(
                "CREATE TABLE users (firstname text, lastname text, age int, email text, city text, PRIMARY KEY (lastname));");
        session.execute(
                "CREATE TABLE users2 (firstname text, lastname text, age int, email text, city text, PRIMARY KEY (lastname));");
    }

    @After
    public void after() {
        cluster.close();
    }

    @Test
    public void testBasic() {
        demoBasic();

        assertEquals(1, InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(1000));
        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());

        String txName = transactionNames.iterator().next();
        DatastoreHelper helper = new DatastoreHelper(CASSANDRA_PRODUCT);
        helper.assertScopedStatementMetricCount(txName, "INSERT", "users", 1);
        helper.assertScopedStatementMetricCount(txName, "SELECT", "users", 3);
        helper.assertScopedStatementMetricCount(txName, "UPDATE", "users", 1);
        helper.assertScopedStatementMetricCount(txName, "DELETE", "users", 1);

        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("INSERT", 1);
        helper.assertUnscopedOperationMetricCount("SELECT", 3);
        helper.assertUnscopedOperationMetricCount("UPDATE", 1);
        helper.assertUnscopedOperationMetricCount("DELETE", 1);
        helper.assertUnscopedStatementMetricCount("INSERT", "users", 1);
        helper.assertUnscopedStatementMetricCount("SELECT", "users", 3);
        helper.assertUnscopedStatementMetricCount("UPDATE", "users", 1);
        helper.assertUnscopedStatementMetricCount("DELETE", "users", 1);

        Collection<TransactionTrace> traces =
                InstrumentationTestRunner.getIntrospector().getTransactionTracesForTransaction(txName);
        assertEquals(1, traces.size());
        TransactionTrace trace = Iterables.getFirst(traces, null);
        assertNotNull(trace);
        assertBasicTraceSegmentAttributes(trace);
    }

    private void assertBasicTraceSegmentAttributes(TransactionTrace trace) {
        List<TraceSegment> children = trace.getInitialTraceSegment().getChildren();
        assertEquals(6, children.size());
        for (TraceSegment child : children) {
            Map<String, Object> tracerAttributes = child.getTracerAttributes();
            assertEquals(hostName, tracerAttributes.get("host"));
            assertEquals(String.valueOf(port), tracerAttributes.get("port_path_or_id"));
            assertEquals(databaseName, tracerAttributes.get("db.instance"));
        }
    }

    @Trace(dispatcher = true)
    public void demoBasic() {
        // Insert one record into the users table
        SimpleStatement insertStatement = new SimpleStatement(
                "/* This is an INSERT query. yay*/ INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')");
        insertStatement.enableTracing();
        session.execute(insertStatement);

        // Use select to get the user we just entered
        session.execute("SELECT * FROM users WHERE lastname='Jones'");

        // Update the same user with a new age
        session.execute("update users set age = 36 where lastname = 'Jones'");

        // Select and show the change
        session.execute("select * from users where lastname='Jones'");

        // Delete the user from the users table
        session.execute("DELETE FROM users WHERE lastname = 'Jones'");

        // Show that the user is gone
        session.execute("SELECT * FROM users");
    }

    @Test
    public void testBatchStatement() {
        demoBatchStatement();
        waitForSegmentEnd();

        DatastoreHelper helper = new DatastoreHelper(CASSANDRA_PRODUCT);
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("BATCH", 1);

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        helper.assertScopedOperationMetricCount(txName, "BATCH", 1);

        Collection<TransactionTrace> traces =
                InstrumentationTestRunner.getIntrospector().getTransactionTracesForTransaction(txName);
        assertEquals(1, traces.size());
        TransactionTrace trace = Iterables.getFirst(traces, null);
        assertNotNull(trace);
        assertBatchStatementTraceSegmentAttributes(trace);
    }

    private void assertBatchStatementTraceSegmentAttributes(TransactionTrace trace) {
        List<TraceSegment> children = trace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        for (TraceSegment child : children) {
            Map<String, Object> tracerAttributes = child.getTracerAttributes();
            assertEquals(hostName, tracerAttributes.get("host"));
            assertEquals(String.valueOf(port), tracerAttributes.get("port_path_or_id"));
            assertEquals(databaseName, tracerAttributes.get("db.instance"));
        }
    }

    @Trace(dispatcher = true)
    public void demoBatchStatement() {
        BatchStatement bs = new BatchStatement();
        bs.add(new SimpleStatement(
                "/* This is an INSERT query. yay*/ INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')"));
        bs.add(new SimpleStatement(
                "INSERT INTO users2 /* nice */ (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')"));
        session.execute(bs);
    }

    @Test
    public void testBatchStatementInsideSimpleStatement() {
        demoBatchStatementInsideSimpleStatement();
        waitForSegmentEnd();

        DatastoreHelper helper = new DatastoreHelper(CASSANDRA_PRODUCT);
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("BATCH", 1);

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        helper.assertScopedOperationMetricCount(txName, "BATCH", 1);

        Collection<TransactionTrace> traces =
                InstrumentationTestRunner.getIntrospector().getTransactionTracesForTransaction(txName);
        assertEquals(1, traces.size());
        TransactionTrace trace = Iterables.getFirst(traces, null);
        assertNotNull(trace);
        assertBatchStatementInsideSimpleStatementTraceSegmentAttributes(trace);
    }

    private void assertBatchStatementInsideSimpleStatementTraceSegmentAttributes(TransactionTrace trace) {
        List<TraceSegment> children = trace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        for (TraceSegment child : children) {
            Map<String, Object> tracerAttributes = child.getTracerAttributes();
            assertEquals(hostName, tracerAttributes.get("host"));
            assertEquals(String.valueOf(port), tracerAttributes.get("port_path_or_id"));
            assertEquals(databaseName, tracerAttributes.get("db.instance"));
        }
    }

    @Trace(dispatcher = true)
    public void demoBatchStatementInsideSimpleStatement() {
        SimpleStatement batch = new SimpleStatement("BEGIN BATCH "
                + " /* some batch inserts */ INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob') "
                + " INSERT INTO users2 /* another comment */ (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')"
                + " APPLY BATCH");
        session.execute(batch);
    }

    @Test
    public void testError() {
        demoError();
        assertEquals(1, InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(1000));
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(2, introspector.getErrors().size());
        assertEquals(2, introspector.getErrorEvents().size());
    }

    @Trace(dispatcher = true)
    public void demoError() {
        try {
            session.execute("SELECT * FORM users WHERE lastname='Jones'");
            Assert.fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testAddListener() throws Exception {
        ResultSet rs = addListernerTest().get();
        ExecutionInfo executionInfo = rs.getExecutionInfo();
        QueryTrace queryTrace = executionInfo.getQueryTrace();
        assertEquals(1, InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(30000));
        DatastoreHelper help = new DatastoreHelper(CASSANDRA_PRODUCT);
        help.assertAggregateMetrics();
        help.assertUnscopedOperationMetricCount("BATCH", 1);
        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        String Txname = transactionNames.iterator().next();
        help.assertScopedOperationMetricCount(Txname, "BATCH", 1);
        Float databaseTime = MetricsHelper.getScopedMetricExclusiveTimeInSec(Txname, help.getOperationMetricName(
                "BATCH"));
        Double timeDiff = ((databaseTime * 1000.0) - ((queryTrace.getDurationMicros()) / 1000.0));
        assertTrue("Database time includes the user callbacks.", timeDiff < 15);
        assertTrue("No time was captured for database.", databaseTime > 0);

        Collection<TransactionTrace> traces =
                InstrumentationTestRunner.getIntrospector().getTransactionTracesForTransaction(Txname);
        assertEquals(1, traces.size());
        TransactionTrace trace = Iterables.getFirst(traces, null);
        assertNotNull(trace);
        assertListenerTraceSegmentAttributes(trace);
    }

    private void assertListenerTraceSegmentAttributes(TransactionTrace trace) {
        List<TraceSegment> children = trace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        for (TraceSegment child : children) {
            Map<String, Object> tracerAttributes = child.getTracerAttributes();
            assertEquals(hostName, tracerAttributes.get("host"));
            assertEquals(String.valueOf(port), tracerAttributes.get("port_path_or_id"));
            assertEquals(databaseName, tracerAttributes.get("db.instance"));
        }
    }

    @Trace(dispatcher = true)
    private ResultSetFuture addListernerTest() {
        Statement batch = new SimpleStatement("BEGIN BATCH "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice1', 25, 'Portland', 'alice@example.com', 'john1') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice2', 25, 'Portland', 'alice@example.com', 'john2') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice3', 25, 'Portland', 'alice@example.com', 'john3') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice4', 25, 'Portland', 'alice@example.com', 'john4') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice5', 25, 'Portland', 'alice@example.com', 'john5') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice6', 25, 'Portland', 'alice@example.com', 'john6') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice7', 25, 'Portland', 'alice@example.com', 'john7') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice77', 25, 'Portland', 'alice@example.com', 'john8') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice8', 25, 'Portland', 'alice@example.com', 'john9') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice9', 25, 'Portland', 'alice@example.com', 'john0') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice10', 25, 'Portland', 'alice@example.com', 'john11') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice11', 25, 'Portland', 'alice@example.com', 'john12') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice12', 25, 'Portland', 'alice@example.com', 'john13') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice13', 25, 'Portland', 'alice@example.com', 'john14') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice14', 25, 'Portland', 'alice@example.com', 'john15') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice15', 25, 'Portland', 'alice@example.com', 'john16') "
                + " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice16', 25, 'Portland', 'alice@example.com', 'john17') "
                + " DELETE FROM users WHERE lastname = 'Alice2' " + " DELETE FROM users WHERE lastname = 'Alice5' "
                + " DELETE FROM users WHERE lastname = 'Alice1' " + " DELETE FROM users WHERE lastname = 'Alice7' "
                + " DELETE FROM users WHERE lastname = 'Alice3' " + " DELETE FROM users WHERE lastname = 'Alice6' "
                + " DELETE FROM users WHERE lastname = 'Alice1' " + " DELETE FROM users WHERE lastname = 'Alice16' "
                + " APPLY BATCH").enableTracing();
        ResultSetFuture result = session.executeAsync(batch);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, cluster.getConfiguration().getPoolingOptions().getInitializationExecutor());
        return result;
    }

    @Test
    public void testCancelFuture() {
        ResultSetFuture future = demoCancelFuture();
        assertTrue("The future is not executed completely", future.isDone());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        DatastoreHelper help = new DatastoreHelper(CASSANDRA_PRODUCT);
        help.assertScopedOperationMetricCount(txName, "BATCH", 1);

        Collection<TransactionTrace> traces =
                InstrumentationTestRunner.getIntrospector().getTransactionTracesForTransaction(txName);
        assertEquals(1, traces.size());
        TransactionTrace trace = Iterables.getFirst(traces, null);
        assertNotNull(trace);
        assertCancelFutureTraceSegmentAttributes(trace);
    }

    private void assertCancelFutureTraceSegmentAttributes(TransactionTrace trace) {
        List<TraceSegment> children = trace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        for (TraceSegment child : children) {
            Map<String, Object> tracerAttributes = child.getTracerAttributes();
            assertNull(tracerAttributes.get("host"));
            assertNull(tracerAttributes.get("port_path_or_id"));
            assertEquals(databaseName, tracerAttributes.get("db.instance"));
        }
    }

    @Trace(dispatcher = true)
    public ResultSetFuture demoCancelFuture() {
        StringBuilder qstring = new StringBuilder("BEGIN BATCH ");
        for (int i = 0; i < 1000; i++) {
            String insert = " INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Alice" + i
                    + "', 25, 'Portland','alice@example.com', 'john1' ";
            qstring.append(insert);
        }
        qstring.append(" APPLY BATCH");
        Statement batch = new SimpleStatement(qstring.toString());
        ResultSetFuture future = session.executeAsync(batch);
        future.cancel(true);
        return future;
    }

    private void waitForSegmentEnd() {
        int ms = 1000;
        final long until = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < until) {
        }
    }
}
