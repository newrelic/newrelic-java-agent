/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.derby_10_11_1_1;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class DerbyDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new DerbyDatabaseVendor();

    private DerbyDatabaseVendor() {
        super("Apache Derby", "derby", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.Derby;
    }

}
