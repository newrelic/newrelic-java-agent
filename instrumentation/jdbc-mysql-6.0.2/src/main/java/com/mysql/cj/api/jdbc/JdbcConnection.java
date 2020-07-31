/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mysql.cj.api.jdbc;

import java.sql.SQLException;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface)
public abstract class JdbcConnection {

    public java.sql.PreparedStatement clientPrepareStatement(String sql) throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement clientPrepareStatement(String sql, int autoGenKeyIndex)
            throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement clientPrepareStatement(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement clientPrepareStatement(String sql, int[] autoGenKeyIndexes)
            throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement clientPrepareStatement(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement clientPrepareStatement(String sql, String[] autoGenKeyColNames)
            throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement serverPrepareStatement(String sql) throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement serverPrepareStatement(String sql, int autoGenKeyIndex)
            throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement serverPrepareStatement(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement serverPrepareStatement(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement serverPrepareStatement(String sql, int[] autoGenKeyIndexes)
            throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

    public java.sql.PreparedStatement serverPrepareStatement(String sql, String[] autoGenKeyColNames)
            throws SQLException {
        java.sql.PreparedStatement preparedStatement = Weaver.callOriginal();
        JdbcHelper.putSql(preparedStatement, sql);
        return preparedStatement;
    }

}
