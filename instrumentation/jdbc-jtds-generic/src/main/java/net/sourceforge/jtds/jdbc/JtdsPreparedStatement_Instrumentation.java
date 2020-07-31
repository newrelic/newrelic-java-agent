/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package net.sourceforge.jtds.jdbc;

import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.sql.Connection;
import java.sql.ResultSet;

@Weave(type = MatchType.BaseClass, originalName = "net.sourceforge.jtds.jdbc.JtdsPreparedStatement")
public abstract class JtdsPreparedStatement_Instrumentation {

    protected final String sql = Weaver.callOriginal();

    protected ParamInfo[] parameters;

    @Trace(leaf = true)
    public ResultSet executeQuery() {
        DatastoreMetrics.noticeSql(getConnection(), sql, getParameterValues());
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public int executeUpdate() {
        DatastoreMetrics.noticeSql(getConnection(), sql, getParameterValues());
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public boolean execute() {
        DatastoreMetrics.noticeSql(getConnection(), sql, getParameterValues());
        return Weaver.callOriginal();
    }

    public abstract Connection getConnection();

    private Object[] getParameterValues() {
        Object[] params = new Object[parameters.length];

        for (int i = 0; i < params.length; i++) {
            params[i] = parameters[i].value;
        }

        return params;
    }
}
