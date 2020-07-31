/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.sqlserver;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class SqlServerDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new SqlServerDatabaseVendor();

    private SqlServerDatabaseVendor() {
        super("Microsoft SQL Server", "sqlserver", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.MSSQL;
    }

}