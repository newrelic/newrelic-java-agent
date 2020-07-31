/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;

public class PreparedStatementExplainPlanExecutor extends DefaultExplainPlanExecutor {

    private final Object[] sqlParameters;

    public PreparedStatementExplainPlanExecutor(SqlTracerExplainInfo tracer, String originalSqlStatement,
            Object[] sqlParameters, RecordSql recordSql) {
        super(tracer, originalSqlStatement, recordSql);
        this.sqlParameters = sqlParameters;
    }

    @Override
    protected Statement createStatement(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    @Override
    protected ResultSet executeStatement(Statement statement, String sql) throws SQLException {
        PreparedStatement preparedStatement = (PreparedStatement) statement;
        setSqlParameters(preparedStatement);
        return preparedStatement.executeQuery();
    }

    private void setSqlParameters(PreparedStatement preparedStatement) throws SQLException {
        if (null == sqlParameters) {
            return;
        }
        int length = 0;
        for (Object sqlParameter : sqlParameters) {
            if (sqlParameter == null) {
                break;
            }
            length++;
        }
        try {
            for (int i = 0; i < length; i++) {
                preparedStatement.setObject(i + 1, sqlParameters[i]);
            }
        } catch (Throwable t) {
            // ignore
        }
    }
}