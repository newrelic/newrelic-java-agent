/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.ExplainPlanSqlInfo;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultExplainPlanExecutorTest {

    @Test
    public void testMultipleStatements() {
        DefaultExplainPlanExecutor explainPlanExecutor = new DefaultExplainPlanExecutor(null, null, null);
        assertTrue(explainPlanExecutor.multipleStatements("SELECT * FROM users; SELECT * FROM users"));
        assertTrue(explainPlanExecutor.multipleStatements("SELECT * FROM users;INSERT INTO table1 (field1,field2) VALUES(1,2);"));
        assertFalse(explainPlanExecutor.multipleStatements("SELECT * FROM users; "));
        assertFalse(explainPlanExecutor.multipleStatements("  SELECT * FROM users;  "));

        // maybe dangerous
        assertTrue(explainPlanExecutor.multipleStatements("  SELECT * FROM users;; "));
    }

    @Test
    public void testMultipleStatementNoExplainPlan() throws SQLException {
        SqlTracerExplainInfo tracer = createSqlTracerInfo("SELECT * FROM users; SELECT * FROM users");
        DefaultExplainPlanExecutor explainPlanExecutor = new DefaultExplainPlanExecutor(tracer, (String) tracer.getSql(), RecordSql.raw);
        explainPlanExecutor.runExplainPlan(null, null, new DatabaseVendor() {
            @Override
            public String getName() {
                return "MyDBVendor";
            }

            @Override
            public String getType() {
                return "MyDB";
            }

            @Override
            public boolean isExplainPlanSupported() {
                return true;
            }

            @Override
            public ExplainPlanSqlInfo getExplainPlanSqlInfo(String sqlToExplain) throws SQLException {
                return new ExplainPlanSqlInfo(sqlToExplain, null);
            }

            @Override
            public boolean isExplainPlanFollowupQueryRequired() {
                return false;
            }

            @Override
            public String getFollowupExplainPlanSql(String statementId) {
                return null;
            }

            @Override
            public Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs, RecordSql recordSql) throws SQLException {
                return null;
            }

            @Override
            public String getExplainPlanFormat() {
                return null;
            }

            @Override
            public DatastoreVendor getDatastoreVendor() {
                return DatastoreVendor.JDBC;
            }
        });

        assertFalse(tracer.hasExplainPlan());
    }

    @Test
    public void testRunExplainPlan() throws SQLException {
        String sql = "select * from test";
        SqlTracerExplainInfo tracer = new TestSqlStatementTracer();
        Assert.assertFalse(tracer.hasExplainPlan());

        DefaultExplainPlanExecutor executor = new DefaultExplainPlanExecutor(tracer,
                sql, RecordSql.raw);

        DatabaseService dbService = Mockito.mock(DatabaseService.class);
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);
        ResultSet results = Mockito.mock(ResultSet.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(results.getMetaData().getColumnCount()).thenReturn(2);
        Mockito.when(statement.executeQuery(Mockito.any())).thenReturn(results);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        DatabaseVendor vendor = Mockito.mock(DatabaseVendor.class);
        Mockito.when(vendor.getExplainPlanSqlInfo(Mockito.any())).thenReturn(new ExplainPlanSqlInfo(sql, null));

        executor.runExplainPlan(dbService, connection, vendor);

        Assert.assertTrue(tracer.hasExplainPlan());
    }

    @Test
    public void testRunExplainPlan_withFollowupQueryRequired_usesFollowupResultSet() throws SQLException {
        String sql = "select * from test";
        String followupSql = "SELECT * FROM PLAN_TABLE WHERE STATEMENT_ID = '42'";
        SqlTracerExplainInfo tracer = new TestSqlStatementTracer();
        Assert.assertFalse(tracer.hasExplainPlan());

        DefaultExplainPlanExecutor executor = new DefaultExplainPlanExecutor(tracer, sql, RecordSql.raw);

        DatabaseService dbService = Mockito.mock(DatabaseService.class);
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);

        // The initial EXPLAIN PLAN statement returns no columns, as is the case for Oracle.
        ResultSet initialResultSet = Mockito.mock(ResultSet.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(initialResultSet.getMetaData().getColumnCount()).thenReturn(0);

        // The followup query against PLAN_TABLE is where the actual explain plan rows come from.
        ResultSet followupResultSet = Mockito.mock(ResultSet.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(followupResultSet.getMetaData().getColumnCount()).thenReturn(2);

        Mockito.when(statement.executeQuery(sql)).thenReturn(initialResultSet);
        Mockito.when(statement.executeQuery(followupSql)).thenReturn(followupResultSet);
        Mockito.when(connection.createStatement()).thenReturn(statement);

        DatabaseVendor vendor = Mockito.mock(DatabaseVendor.class);
        Mockito.when(vendor.getExplainPlanSqlInfo(Mockito.any())).thenReturn(new ExplainPlanSqlInfo(sql, "42"));
        Mockito.when(vendor.isExplainPlanFollowupQueryRequired()).thenReturn(true);
        Mockito.when(vendor.getFollowupExplainPlanSql("42")).thenReturn(followupSql);

        executor.runExplainPlan(dbService, connection, vendor);

        Assert.assertTrue(tracer.hasExplainPlan());
        Mockito.verify(vendor).parseExplainPlanResultSet(2, followupResultSet, RecordSql.raw);
    }

    private SqlTracerExplainInfo createSqlTracerInfo(final String sql) {
        return new SqlTracerExplainInfo() {
            public Object[] explainPlan = null;

            @Override
            public Object getSql() {
                return sql;
            }

            @Override
            public boolean hasExplainPlan() {
                return explainPlan != null;
            }

            @Override
            public void setExplainPlan(Object... explainPlan) {
                this.explainPlan = explainPlan;
            }

            @Override
            public ExplainPlanExecutor getExplainPlanExecutor() {
                return null;
            }
        };
    }

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

}