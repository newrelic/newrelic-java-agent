/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.postgresql_80_jdbc3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;
import com.newrelic.agent.bridge.datastore.RecordSql;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class PostgresDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new PostgresDatabaseVendor();

    private final Set<String> explainKeysToPreserve;

    private PostgresDatabaseVendor() {
        super("PostgreSQL", "postgresql", true);
        this.explainKeysToPreserve = getExplainKeysToPreserve();
    }

    /**
     * The keys that should _not_ be obfuscated from an explain plan.
     */
    private Set<String> getExplainKeysToPreserve() {
        Set<String> explainKeys = new HashSet<>(26, 1.0f);
        explainKeys.add("Plan");
        explainKeys.add("Plans");
        explainKeys.add("Node Type");
        explainKeys.add("Alias");
        explainKeys.add("Startup Cost");
        explainKeys.add("Total Cost");
        explainKeys.add("Plan Rows");
        explainKeys.add("Plan Width");
        explainKeys.add("Parent Relationship");
        explainKeys.add("Join Type");
        explainKeys.add("Group Key");
        explainKeys.add("Sort Key");
        explainKeys.add("Relation Name");
        explainKeys.add("Sort Method");
        explainKeys.add("Sort Space Used");
        explainKeys.add("Sort Space Type");
        explainKeys.add("Scan Direction");
        explainKeys.add("Index Name");
        explainKeys.add("Actual Startup Time");
        explainKeys.add("Actual Total Time");
        explainKeys.add("Actual Rows");
        explainKeys.add("Actual Loops");
        explainKeys.add("Triggers");
        explainKeys.add("Total Runtime");
        explainKeys.add("Strategy");
        return explainKeys;
    }

    /**
     * We use the json format of explain plans which is only supported in Postgres 9.
     * It's easier to prevent obfuscation when it's in this format.
     */
    public String getExplainPlanSql(String sql) throws SQLException {
        return "EXPLAIN (FORMAT JSON) " + sql;
    }

    @Override
    public String getExplainPlanFormat() {
        return "json";
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.Postgres;
    }

    /**
     * Use the json explain plan format, parse it into objects, scrub sensitive information and
     * then convert the data back to json.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs,
            RecordSql recordSql) throws SQLException {
        if (rs.next()) {
            String json = rs.getObject(1).toString();

            try {
                JSONArray parse = (JSONArray) new JSONParser().parse(json);
                if (RecordSql.obfuscated.equals(recordSql)) {
                    scrubPlan((Map<String, Object>) parse.get(0));
                }

                return Arrays.<Collection<Object>> asList(parse);
            } catch (ParseException e) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, "Unable to parse explain plan: {0}", e.toString());
                return Arrays.<Collection<Object>> asList(Arrays.<Object>asList("Unable to parse explain plan"));
            }
        } else {
            return Arrays.<Collection<Object>> asList(Arrays.<Object> asList("No rows were returned by the explain plan"));
        }
    }

    @SuppressWarnings("unchecked")
    private void scrubPlan(Map<String, Object> plan) {

        Map<String, Object> innerPlan = (Map<String, Object>) plan.get("Plan");
        if (innerPlan != null) {
            scrubPlan(innerPlan);
        } else {
            JSONArray plans = (JSONArray) plan.get("Plans");
            if (plans != null) {
                for (Object childPlan : plans) {
                    scrubPlan((Map<String, Object>) childPlan);
                }
            }
        }

        for (Map.Entry<String, Object> entry : plan.entrySet()) {
            if (!explainKeysToPreserve.contains(entry.getKey())) {
                entry.setValue("?");
            }
        }
    }
}
