/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

public abstract class JdbcDatabaseVendor implements DatabaseVendor {

    protected final String name;
    protected String type;
    protected boolean explainPlanSupported;

    public JdbcDatabaseVendor(String name, String type, boolean explainSupported) {
        this.name = name;
        this.type = type;
        this.explainPlanSupported = explainSupported;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isExplainPlanSupported() {
        return explainPlanSupported;
    }

    public String getExplainPlanSql(String sql) throws SQLException {
        if (!isExplainPlanSupported()) {
            throw new SQLException("Unable to run explain plans for " + getName() + " databases");
        }
        return "EXPLAIN " + sql;
    }

    /**
     * Parse an explain plan result set and return a collection of collections representing it.
     *
     * @param recordSql
     */
    public Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs, RecordSql recordSql)
            throws SQLException {
        Collection<Collection<Object>> explains = new LinkedList<>();
        while (rs.next()) {
            Collection<Object> values = new LinkedList<>();
            for (int i = 1; i <= columnCount; i++) {
                Object obj = rs.getObject(i);
                values.add(obj == null ? "" : obj.toString());
            }
            explains.add(values);
        }
        return explains;
    }

    /**
     * The format of the explain plan. Right now this is 'text' or 'json'.
     *
     * @return
     */
    public String getExplainPlanFormat() {
        return "text";
    }

    public abstract DatastoreVendor getDatastoreVendor();
}
