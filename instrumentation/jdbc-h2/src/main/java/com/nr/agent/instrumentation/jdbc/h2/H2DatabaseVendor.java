/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.h2;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class H2DatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new H2DatabaseVendor();

    private H2DatabaseVendor() {
        super("H2 Database Engine", "h2", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.H2;
    }

}
