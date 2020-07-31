/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.sybase;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class SybaseDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new SybaseDatabaseVendor();

    private SybaseDatabaseVendor() {
        super("Sybase", "sybase", false); // Explain plans not currently supported.
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.Sybase;
    }

}
