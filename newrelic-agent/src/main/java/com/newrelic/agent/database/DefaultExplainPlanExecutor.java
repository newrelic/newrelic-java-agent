/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.logging.Level;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;

public class DefaultExplainPlanExecutor implements ExplainPlanExecutor {

    private SqlTracerExplainInfo tracer;
    private final String originalSqlStatement;
    private final RecordSql recordSql;

    public DefaultExplainPlanExecutor(SqlTracerExplainInfo tracer, String originalSqlStatement, RecordSql recordSql) {
        this.originalSqlStatement = originalSqlStatement;
        this.tracer = tracer;
        this.recordSql = recordSql;
    }

    private Object[] getExplainPlanFromResultSet(DatabaseVendor vendor, ResultSet rs, RecordSql recordSql)
            throws SQLException {
        int columnCount = rs.getMetaData().getColumnCount();
        if (columnCount > 0) {
            Collection<Collection<Object>> explains = vendor.parseExplainPlanResultSet(columnCount, rs, recordSql);
            return new Object[] { explains };
        }
        return null;
    }

    @Override
    public void runExplainPlan(DatabaseService databaseService, Connection connection, DatabaseVendor vendor)
            throws SQLException {
        String sql = originalSqlStatement;
        try {
            sql = vendor.getExplainPlanSql(sql);
        } catch (SQLException e) {
            tracer.setExplainPlan(e.getMessage());
            return;
        }

        if (multipleStatements(sql)){
            Agent.LOG.finer("SQL string may contain multiple statements. Not running explain plan");
            return;
        }

        Agent.LOG.finer("Running explain: " + sql);
        ResultSet resultSet = null;
        Statement statement = null;
        Object[] explainPlan = null;
        try {
            statement = createStatement(connection, sql);
            resultSet = executeStatement(statement, sql);
            explainPlan = getExplainPlanFromResultSet(vendor, resultSet, recordSql);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINER, "explain plan error", e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (Exception e) {
                    Agent.LOG.log(Level.FINER, "Unable to close result set", e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                    Agent.LOG.log(Level.FINER, "Unable to close statement", e);
                }
            }
        }
        if (explainPlan != null) {
            tracer.setExplainPlan(explainPlan);
        }
    }

    /**
     * Check if a sql string may contain multiple statements.
     */
    @VisibleForTesting
    protected boolean multipleStatements(String sql) {
        // Check if a sql string may contain multiple statements.
        // This should prevent the agent from running an explain plan with multiple queries
        //  like this:
        //  EXPLAIN SELECT * FROM users; SELECT * FROM users
        String trimmedSql = sql.trim();
        int index = trimmedSql.indexOf(";");
        return index >= 0 && index < trimmedSql.length() - 1;
    }

    protected ResultSet executeStatement(Statement statement, String sql) throws SQLException {
        return statement.executeQuery(sql);
    }

    protected Statement createStatement(Connection connection, String sql) throws SQLException {
        return connection.createStatement();
    }
}