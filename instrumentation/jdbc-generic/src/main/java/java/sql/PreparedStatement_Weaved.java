/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.sql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.math.BigDecimal;
import java.util.logging.Level;

@Weave(originalName = "java.sql.PreparedStatement", type = MatchType.Interface)
public abstract class PreparedStatement_Weaved {

    @NewField
    private Object[] params;

    @NewField
    String preparedSql;

    @Trace(leaf = true)
    public ResultSet executeQuery() throws SQLException {
        if (preparedSql == null) {
            preparedSql = JdbcHelper.getSql((Statement) this);
        }

        long start = System.currentTimeMillis();
        DatastoreMetrics.noticeSql(getConnection(), preparedSql, params);
        NewRelic.getAgent().getLogger().log(Level.INFO, "SQL_TRACE: executeQuery() - noticeSql(); elapsed time: {1}",  System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        ResultSet resultSet = Weaver.callOriginal();
        NewRelic.getAgent().getLogger().log(Level.INFO, "SQL_TRACE: executeQuery() - callOriginal(); elapsed time: {1}",  System.currentTimeMillis() - start);

        return resultSet;
    }

    @Trace(leaf = true)
    public int executeUpdate() throws SQLException {
        if (preparedSql == null) {
            preparedSql = JdbcHelper.getSql((Statement) this);
        }

        long start = System.currentTimeMillis();
        DatastoreMetrics.noticeSql(getConnection(), preparedSql, params);
        NewRelic.getAgent().getLogger().log(Level.INFO, "SQL_TRACE: executeUpdate() - noticeSql(); elapsed time: {1}",  System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        int result = Weaver.callOriginal();
        NewRelic.getAgent().getLogger().log(Level.INFO, "SQL_TRACE: executeUpdate() - callOriginal(); elapsed time: {1}",  System.currentTimeMillis() - start);

        return result;
    }

    @Trace(leaf = true)
    public boolean execute() throws SQLException {
        if (preparedSql == null) {
            preparedSql = JdbcHelper.getSql((Statement) this);
        }

        long start = System.currentTimeMillis();
        DatastoreMetrics.noticeSql(getConnection(), preparedSql, params);
        NewRelic.getAgent().getLogger().log(Level.INFO, "SQL_TRACE: execute() - noticeSql(); elapsed time: {1}",  System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        boolean result = Weaver.callOriginal();
        NewRelic.getAgent().getLogger().log(Level.INFO, "SQL_TRACE: executeUpdate() - callOriginal(); elapsed time: {1}",  System.currentTimeMillis() - start);

        return result;
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        setParamValue(parameterIndex, "null");
        Weaver.callOriginal();
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        setParamValue(parameterIndex, x);
        Weaver.callOriginal();
    }

    public void clearParameters() throws SQLException {
        params = new Object[0];

        Weaver.callOriginal();
    }

    public abstract Connection getConnection() throws SQLException;

    private void setParamValue(int index, Object value) {
        if (params == null) {
            params = new Object[1];
        }

        index--;
        if (index < 0) {
            AgentBridge.getAgent().getLogger().log(Level.FINER,
                    "Unable to store a prepared statement parameter because the index < 0");
            return;
        } else if (index >= params.length) {
            params = JdbcHelper.growParameterArray(params, index);
        }
        params[index] = value;
    }

}
