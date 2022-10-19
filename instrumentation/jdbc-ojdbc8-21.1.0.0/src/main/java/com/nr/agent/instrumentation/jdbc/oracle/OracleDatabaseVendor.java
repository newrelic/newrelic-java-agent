/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.oracle;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

import java.sql.SQLException;

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
}
