/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.hsqldb;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class HSQLDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new HSQLDatabaseVendor();

    private HSQLDatabaseVendor() {
        super("HyperSQL Database Engine", "hsqldb", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.HSQLDB;
    }

}
