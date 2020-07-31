/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.inet.oranxo;

import java.sql.SQLException;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class INetOracleDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new INetOracleDatabaseVendor();

    private INetOracleDatabaseVendor() {
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