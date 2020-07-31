/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.jtds;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class JtdsDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new JtdsDatabaseVendor();

    private JtdsDatabaseVendor() {
        super("Microsoft SQL Server", "sqlserver", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.MSSQL;
    }

}