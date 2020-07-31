/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.inet.merlia;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class MerliaDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new MerliaDatabaseVendor();

    private MerliaDatabaseVendor() {
        super("Microsoft SQL Server", "sqlserver", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.MSSQL;
    }

}