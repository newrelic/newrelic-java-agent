/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.db2;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class Db2DatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new Db2DatabaseVendor();

    private Db2DatabaseVendor() {
        super("DB2", "db2", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.IBMDB2;
    }

}