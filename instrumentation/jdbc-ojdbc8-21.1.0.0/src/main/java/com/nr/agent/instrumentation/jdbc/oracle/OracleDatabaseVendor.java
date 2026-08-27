/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
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
        // DBMS_XPLAN.DISPLAY renders PLAN_TABLE's rows for this statement id into Oracle's own
        // canonical, human-readable plan report (one line of text per row).
        return "SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY('PLAN_TABLE', '" + statementId + "', 'TYPICAL'))";
    }

    @Override
    public Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs, RecordSql recordSql)
            throws SQLException {
        // Note: unlike the other DatabaseVendor implementations, recordSql/obfuscation isn't
        // applied here yet. DBMS_XPLAN.DISPLAY's predicate info is embedded inline in free-form
        // text lines rather than a discrete column, so it can't be scrubbed by clearing a value.
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
