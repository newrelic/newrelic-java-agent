/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

public class UnknownDatabaseVendor extends JdbcDatabaseVendor {

    public static final DatabaseVendor INSTANCE = new UnknownDatabaseVendor();

    private UnknownDatabaseVendor() {
        super("Unknown", DatastoreVendor.JDBC.name(), false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.JDBC;
    }
}