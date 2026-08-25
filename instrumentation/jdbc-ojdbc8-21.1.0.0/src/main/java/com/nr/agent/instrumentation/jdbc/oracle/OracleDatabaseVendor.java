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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class OracleDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new OracleDatabaseVendor();
    private static final AtomicInteger EXPLAIN_PLAN_ID_GENERATOR = new AtomicInteger(0);

    /**
     * PLAN_TABLE has ~30 columns; these are the ones that we actually care about
     */
    private static final List<String> EXPLAIN_PLAN_COLUMNS = Arrays.asList(
            "ID", "OPERATION", "OPTIONS", "OBJECT_NAME", "COST", "CARDINALITY", "BYTES",
            "ACCESS_PREDICATES", "FILTER_PREDICATES");

    /**
     * These columns can contain literal values from the original SQL's WHERE clause, so
     * we obfuscate these
     */
    private static final Set<String> PREDICATE_COLUMNS = new HashSet<>(
            Arrays.asList("ACCESS_PREDICATES", "FILTER_PREDICATES"));

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
        return "SELECT * FROM PLAN_TABLE WHERE STATEMENT_ID = '" + statementId + "'";
    }

    @Override
    public Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs, RecordSql recordSql)
            throws SQLException {
        boolean obfuscate = RecordSql.obfuscated.equals(recordSql);
        Collection<Collection<Object>> explainPlan = new LinkedList<>();
        while (rs.next()) {
            Collection<Object> row = new LinkedList<>();

            // This replaces the entire predicate column value with a "?"
            // which matches the behavior of postgres explain plans
            for (String column : EXPLAIN_PLAN_COLUMNS) {
                if (obfuscate && PREDICATE_COLUMNS.contains(column)) {
                    row.add("?");
                    continue;
                }
                Object value = rs.getObject(column);
                row.add(value == null ? "" : value.toString());
            }
            explainPlan.add(row);
        }

        return explainPlan;
    }
}
