/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
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
            public String getExplainPlanSql(String sql) throws SQLException {
                return sql;
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
}