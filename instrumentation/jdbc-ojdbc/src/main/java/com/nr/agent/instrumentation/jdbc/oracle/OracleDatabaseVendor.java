/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.oracle;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TraceMetadata;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class OracleDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new OracleDatabaseVendor();

    private OracleDatabaseVendor() {
        super("Oracle", "oracle", false); // Explain plans not currently supported.
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.Oracle;
    }

    @Override
    public String getExplainPlanSql(String sql) throws SQLException {
        return "EXPLAIN PLAN FOR " + sql;
    }

    // TODO     These methods will be moved to the JdbcDatabaseVendor class if this implementation
    // TODO     becomes permanent
    public boolean isNativeMetadataCorrelationSupported() {
        return true;
    }

    public void executeNativeMetadataCorrelationStatements(Connection connection) throws SQLException {
        if (isNativeMetadataCorrelationSupported()) {
            throw new SQLException("Unable to run native correlation statements for " + getName() + " databases");
        }

        TraceMetadata traceMetadata = NewRelic.getAgent().getTraceMetadata();
        try {
            try (CallableStatement stmt = connection.prepareCall("{call DBMS_SESSION.SET_IDENTIFIER(?)}")) {
                stmt.setString(1, traceMetadata.getTraceId() + ":" + traceMetadata.getSpanId());
                stmt.execute();
            }

            try (CallableStatement stmt = connection.prepareCall("{call DBMS_APPLICATION_INFO.SET_MODULE(?, ?)}")) {
                stmt.setString(1, NewRelic.getAgent().getConfig().getValue("app_name"));
                stmt.setString(2, NewRelic.getAgent().getTransaction().getTracedMethod().getMetricName());
                stmt.execute();
            }
        } catch (SQLException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Error occurred executing native metadata correlation statements: {0}" +
                    " SQL state: {1}  SQL error code: {2}", e.getMessage(), e.getSQLState(), e.getErrorCode());
        }
    }
}
