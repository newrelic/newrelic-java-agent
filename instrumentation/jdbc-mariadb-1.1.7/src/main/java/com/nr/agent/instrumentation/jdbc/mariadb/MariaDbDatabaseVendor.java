/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.mariadb;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class MariaDbDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new MariaDbDatabaseVendor();

    private MariaDbDatabaseVendor() {
        super("MariaDB", "mysql", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.MySQL;
    }

}