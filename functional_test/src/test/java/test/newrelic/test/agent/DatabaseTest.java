/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.ConnectionConfigListener;
import com.newrelic.agent.DatabaseHelper;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.database.DefaultExplainPlanExecutor;
import com.newrelic.agent.database.ExplainPlanExecutor;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.Trace;
import org.apache.commons.dbcp.DelegatingConnection;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import sql.DummyConnection;
import sql.DummyStatement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class DatabaseTest {

    public class TestSqlStatementTracer implements SqlTracerExplainInfo {

        Object[] explainPlan;

        @Override
        public Object getSql() {
            return null;
        }

        @Override
        public void setExplainPlan(Object... explainPlan) {
            this.explainPlan = explainPlan;
        }

        @Override
        public boolean hasExplainPlan() {
            return explainPlan != null;
        }

        @Override
        public ExplainPlanExecutor getExplainPlanExecutor() {
            return null;
        }
    }

    @BeforeClass
    public static void initDatabase() throws Exception {
        DatabaseHelper.initDatabase(DatabaseTest.class);
    }

    @AfterClass
    public static void shutdownDatabase() {
        DatabaseHelper.shutdownDatabase(DatabaseTest.class);
    }

    @Test
    public void testProxy() {
        // this will blow up if we instrument the proxy class.
        Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class },
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return null;
                    }
                });
        Assert.assertTrue(true);
    }

    @Test
    public void testExplainPlan() throws SQLException {
        TestSqlStatementTracer tracer = new TestSqlStatementTracer();
        DefaultExplainPlanExecutor explainExecutor = new DefaultExplainPlanExecutor(tracer, "select * from test",
                RecordSql.raw);
        // explainSet[0] = explainPlan[0].toString().matches(".*Unable to run explain plan.*");

        Connection connection = DatabaseHelper.getConnection();
        explainExecutor.runExplainPlan(ServiceFactory.getDatabaseService(), connection,
                new MockDerbyDatabaseVendor("Apache Derby", "derby", false));
        Assert.assertNotNull(tracer.explainPlan);
        Assert.assertEquals(1, tracer.explainPlan.length);
        Assert.assertEquals("Unable to run explain plans for Apache Derby databases", tracer.explainPlan[0]);
        // Assert.assertTrue(explainSet[0]);
    }

    @Test
    public void test() throws Throwable {
        final String applicationName = "App";

        ExecuteServlet servlet = new ExecuteServlet();

        AgentHelper.invokeServlet(servlet, "", applicationName, "/database");

        Set<String> metrics = AgentHelper.getMetrics();

        AgentHelper.verifyMetrics(metrics, // MetricNames.DISPATCHER,
                "Servlet/" + ExecuteServlet.class.getName() + "/service");
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "insert");
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "select");
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "delete");
    }

    @Test
    public void addBatch() throws Exception {
        final String applicationName = "App";
        new StubServlet(applicationName) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                Connection connection = DatabaseHelper.getConnection();
                Statement stmt = connection.createStatement();
                for (int i = 0; i < 10; i++) {
                    stmt.addBatch(MessageFormat.format("insert into test values ({0}, ''test{0}'')", i));
                }
                stmt.executeBatch();
            }
        };

        // AgentHelper.verifyMetrics(applicationName, "Database/test/select", "Database/test2/delete",
        // "Database/test3/delete");
    }

    @Test
    public void execute() throws Exception {
        final String applicationName = "App";
        new StubServlet(applicationName) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                Connection connection = DatabaseHelper.getConnection();
                connection.createStatement().execute("select * from test", Statement.NO_GENERATED_KEYS);
                connection.createStatement().execute("delete from test2", new int[0]);
                connection.createStatement().execute("delete from test3", new String[0]);
            }
        };

        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "select");
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test2", "delete");
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test3", "delete");
    }

    @Test
    public void executeWithoutTransaction() throws Exception {
        Connection connection = DatabaseHelper.getConnection();
        connection.createStatement().executeQuery("select * from test");

        AgentHelper.getMetrics(AgentHelper.getDefaultStatsEngine());
    }

    @Test
    public void sqlException() throws Exception {
        final String BAD_SQL = "this sql is no good!";
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);
        final Map<String, Object> params = new HashMap<>();
        try {
            new StubServlet("dude") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                    Connection connection = DatabaseHelper.getConnection();
                    Statement stmt = connection.createStatement();

                    try {
                        stmt.executeQuery(BAD_SQL);
                    } finally {
                        params.putAll(Transaction.getTransaction().getIntrinsicAttributes());
                    }
                }
            };
        } catch (Exception e) {
        }
        Assert.assertEquals(2, txs.size());
        TransactionData transactionData = txs.get(1);

        Assert.assertEquals(500, transactionData.getResponseStatus());
        Assert.assertEquals(BAD_SQL, params.get("sql"));
    }

    @Test
    public void executeQuery() throws Exception {
        final String applicationName = "App";
        new StubServlet(applicationName) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                Connection connection = DatabaseHelper.getConnection();
                ResultSet rs = connection.createStatement().executeQuery("select * from test");

                Transaction tx = Transaction.getTransaction();
                Tracer lastTracer = tx.getTransactionActivity().getLastTracer();
                List<Tracer> children = AgentHelper.getChildren(lastTracer);

                Assert.assertFalse(children.isEmpty());
                Assert.assertTrue(children.get(children.size() - 1).isLeaf());

                while (rs.next()) {

                }
            }
        };
        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "select");
        AgentHelper.verifyMetrics(metrics, "Datastore/ResultSet");
    }

    @Test
    public void innerSelect() throws Exception {
        _innerSelect();

        // we parse out the table name using the ResultSetMetaData
        AgentHelper.verifyDatastoreMetrics(AgentHelper.getMetrics(), DatastoreVendor.Derby, "test", "select");
    }

    @Trace(dispatcher = true)
    private void _innerSelect() throws SQLException {
        Connection connection = DatabaseHelper.getConnection();
        connection.createStatement().executeQuery("select dude.* from (select * from test) as dude");
    }

    @Test
    public void time() throws Exception {
        final long now = System.currentTimeMillis();
        final int count = 500;
        final String applicationName = "App";
        new StubServlet(applicationName) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                Connection connection = DatabaseHelper.getConnection();
                PreparedStatement statement = connection.prepareStatement("select * from test where name = ?");
                for (int i = 0; i < count; i++) {
                    // ResultSet rs = connection.createStatement().executeQuery("select * from test");
                    statement.setString(1, "test");
                    statement.executeQuery();
                }
            }
        };
        System.err.println(count + " invocations of executeQuery took " + (System.currentTimeMillis() - now));
        AgentHelper.verifyDatastoreMetrics(AgentHelper.getMetrics(), DatastoreVendor.Derby, "test", "select");
    }

    /**
     * Tests instrumentation for 4 different executeUpdate method signatures.
     * 
     * @throws Exception
     */
    @Test
    public void executeUpdate() throws Exception {
        final String applicationName = "App";
        new StubServlet(applicationName) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                Connection connection = DatabaseHelper.getConnection();
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("insert into test values (999, 'test')");
                stmt.executeUpdate("delete from test", new String[0]);
                stmt.executeUpdate("update test set name = 'dude' where id = 999", Statement.NO_GENERATED_KEYS);
                stmt.executeUpdate("insert into test2 values (999, 'testies')", new int[0]);
            }
        };
        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "insert");
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test2", "insert");
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "delete");
        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "update");
    }

    @Test
    public void insert() throws Exception {
        new StubServlet("Dude") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                Connection connection = DatabaseHelper.getConnection();
                List<String> tableNames = Arrays.asList("test", "test2");
                for (String tableName : tableNames) {
                    connection.createStatement().executeUpdate("delete from " + tableName);
                }
                int rows = 10;
                for (int i = 0; i < rows; i++) {
                    for (String tableName : tableNames) {
                        String sql = MessageFormat.format("insert into {0} values ({1}, ''test'')", tableName, i);
                        connection.createStatement().executeUpdate(sql);
                    }
                }

                // cartesian product
                ResultSet rs = connection.createStatement().executeQuery("Select * from test, test2");
                int count = 0;
                while (rs.next()) {
                    count++;
                }

                Assert.assertEquals(rows * rows, count);

            }
        };

        // AgentHelper.blockScheduledTasks()

        /*
         * TransactionTrace transactionTrace = ServiceManagerFactory.getTransactionTraceService().getTransactionTrace();
         * Assert.assertNotNull(transactionTrace); Map<ConnectionFactory, List<Tracer>> sqlTracers =
         * transactionTrace.getSqlTracers(); System.err.println(sqlTracers);
         */
    }

    @Test
    public void show() throws Exception {
        Statement stmt = new DummyStatement(new DummyConnection());
        stmt.executeQuery("show tables");

        Set<String> metrics = AgentHelper.getMetrics(AgentHelper.getDefaultStatsEngine());

        for (String metric : metrics) {
            if (metric.startsWith("Database") && metric.endsWith("show")) {
                Assert.fail();
            }
        }

    }

    @Test
    public void useDelegatingPreparedStatement() throws Exception {
        final String applicationName = "App";

        ((ConnectionConfigListener) ServiceFactory.getConfigService()).connected(
                ServiceFactory.getRPMServiceManager().getRPMService(), new HashMap<String, Object>() {
                    {
                        put("transaction_tracer.record_sql", "raw");
                    }
                });

        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(null);
        Assert.assertEquals(SqlObfuscator.RAW_SETTING, agentConfig.getTransactionTracerConfig().getRecordSql());
        Assert.assertEquals(SqlObfuscator.RAW_SETTING, agentConfig.getRequestTransactionTracerConfig().getRecordSql());

        HttpServlet servlet = new PreparedStatementServlet(true, 1);

        AgentHelper.invokeServlet(servlet, "", applicationName, "/database/test");

        Set<String> metrics = AgentHelper.getMetrics(AgentHelper.getDefaultStatsEngine());

        Assert.assertTrue(metrics.toString(), metrics.size() > 0);

        AgentHelper.verifyDatastoreMetrics(metrics, DatastoreVendor.Derby, "test", "select");
    }

    @Test
    public void usePreparedStatement() throws Exception {
        final String applicationName = "App";

        ((ConnectionConfigListener) ServiceFactory.getConfigService()).connected(
                ServiceFactory.getRPMServiceManager().getRPMService(), new HashMap<String, Object>() {
                    {
                        put("transaction_tracer.record_sql", "raw");
                    }
                });

        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(null);
        Assert.assertEquals(SqlObfuscator.RAW_SETTING, agentConfig.getTransactionTracerConfig().getRecordSql());
        Assert.assertEquals(SqlObfuscator.RAW_SETTING, agentConfig.getRequestTransactionTracerConfig().getRecordSql());

        HttpServlet servlet = new PreparedStatementServlet();

        AgentHelper.invokeServlet(servlet, "", applicationName, "/database/test");

        AgentHelper.verifyDatastoreMetrics(AgentHelper.getMetrics(), DatastoreVendor.Derby, "test", "select");
    }

    @Test
    public void preparedStatement() throws Exception {
        final String applicationName = "App";
        StubServlet servlet = new StubServlet(applicationName) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                Connection connection = DatabaseHelper.getConnection();
                PreparedStatement statement = connection.prepareStatement("select * from test where name = ?");
                statement.setString(1, "test");
                new DelegatingPreparedStatement(statement).execute();
            }
        };
        AgentHelper.invokeServlet(servlet, "", applicationName, "/database/test");

        // AgentHelper.verifyMetrics(applicationName, "Database/test/select");
    }

    @Test
    public void sql() throws Exception {
        Connection connection = DatabaseHelper.getConnection();
        PreparedStatement statement = connection.prepareStatement("select * from test where name = ?");
        statement.setString(1, "test");
        statement.executeQuery();
        final String BAD_SQL = "this sql is no good!";
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);
        final Map<String, Object> params = new HashMap<>();
        try {
            new StubServlet("dude") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                    Connection connection = DatabaseHelper.getConnection();
                    Statement stmt = connection.createStatement();

                    try {
                        stmt.executeQuery(BAD_SQL);
                    } finally {
                        params.putAll(Transaction.getTransaction().getIntrinsicAttributes());
                    }
                }
            };
        } catch (Exception e) {
        }
        Assert.assertEquals(2, txs.size());
        TransactionData transactionData = txs.get(1);

        Assert.assertEquals(500, transactionData.getResponseStatus());
        Assert.assertEquals(BAD_SQL, params.get("sql"));
    }

    @Test
    public void preparedCall() throws Exception {
        Agent.LOG.log(Level.FINE, "Starting preparedCall");
        final String applicationName = "App";
        StubServlet servlet = new StubServlet(applicationName) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                Connection connection = DatabaseHelper.getConnection();
                PreparedStatement statement = connection.prepareCall("select * from test where name = ?");
                statement.setString(1, "test");
                statement.execute();
                int fetchSize = statement.getResultSet().getFetchSize();
                System.err.println(fetchSize);
            }
        };
        AgentHelper.invokeServlet(servlet, "", applicationName, "/database/test");

        AgentHelper.verifyDatastoreMetrics(AgentHelper.getMetrics(), DatastoreVendor.Derby, "test", "select");
    }

    public class PreparedStatementServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private final boolean delegate;
        private final int iterations;

        public PreparedStatementServlet(boolean delegate, int iterations) {
            this.delegate = delegate;
            this.iterations = iterations;
        }

        public PreparedStatementServlet() {
            this(false, 1);
        }

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                IOException {

            try {
                Connection connection = DatabaseHelper.getConnection();
                if (delegate) {
                    connection = new DelegatingConnection(connection);
                }
                PreparedStatement prepStmt = connection.prepareStatement("select * from test where id = ?");
                PreparedStatement origPrepStmt = prepStmt;
                try {
                    for (int i = 0; i < iterations; i++) {
                        prepStmt = origPrepStmt;
                        prepStmt.setInt(1, i);
                        prepStmt.executeQuery();
                        if (delegate) {
                            prepStmt = (PreparedStatement) ((org.apache.commons.dbcp.DelegatingPreparedStatement) prepStmt).getDelegate();
                        }
                    }
                } finally {
                    prepStmt.close();
                }
            } catch (Throwable e) {
                throw new ServletException(e);
            }
        }
    }

    public class ExecuteServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                IOException {

            try {
                Connection connection = DatabaseHelper.getConnection();
                for (int i = 0; i < 10; i++) {
                    connection.createStatement().execute(
                            MessageFormat.format("insert into test values ({0}, ''test{0}'')", i));
                }
                Statement statement = connection.createStatement();
                statement.execute("select * from test");
                statement.execute("delete from test");
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
    }
    
    public class MockDerbyDatabaseVendor extends JdbcDatabaseVendor {

        public MockDerbyDatabaseVendor(String name, String type, boolean explainSupported) {
            super(name, type, explainSupported);
        }

        @Override
        public DatastoreVendor getDatastoreVendor() {
            return DatastoreVendor.Derby;
        }

    }
}
