/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.oracle;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.ExplainPlanSqlInfo;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;
import com.newrelic.agent.bridge.datastore.RecordSql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class OracleDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new OracleDatabaseVendor();
    private static final AtomicInteger EXPLAIN_PLAN_ID_GENERATOR = new AtomicInteger(0);

    private OracleDatabaseVendor() {
        super("Oracle", "oracle", true);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.Oracle;
    }

    @Override
    public ExplainPlanSqlInfo getExplainPlanSqlInfo(String sqlToExplain) throws SQLException {
        String statementId = Integer.toString(EXPLAIN_PLAN_ID_GENERATOR.getAndIncrement());
        return new ExplainPlanSqlInfo("EXPLAIN PLAN SET STATEMENT_ID = '" + statementId + "' FOR " + sqlToExplain, statementId);
    }

    @Override
    public boolean isExplainPlanFollowupQueryRequired() {
        return true;
    }

    @Override
    public String getFollowupExplainPlanSql(String statementId) {
        // Using DBMS_XPLAN.DISPLAY transforms the PLAN_TABLE rows into
        // human-readable explain plan text
        return "SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY('PLAN_TABLE', '" + statementId + "', 'TYPICAL'))";
    }

    @Override
    public Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs, RecordSql recordSql)
            throws SQLException {
        StringBuilder planText = new StringBuilder();
        while (rs.next()) {
            if (planText.length() > 0) {
                planText.append('\n');
            }
            planText.append(rs.getString(1));
        }

        return Collections.singletonList(Collections.singletonList(planText.toString()));
    }
}
