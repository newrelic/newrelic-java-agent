/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.mysql;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class MySQLDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new MySQLDatabaseVendor();

    private MySQLDatabaseVendor() {
        super("MySQL", "mysql", true);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.MySQL;
    }
}
