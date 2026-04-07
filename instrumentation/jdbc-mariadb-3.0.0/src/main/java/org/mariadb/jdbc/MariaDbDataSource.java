/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.mariadb.jdbc;

import java.sql.Connection;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.mariadb.MariaDbDatabaseVendor;

@Weave
public abstract class MariaDbDataSource {

    public Connection getConnection() {
        JdbcHelper.putVendor(getClass(), MariaDbDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

    public Connection getConnection(String user, String password) {
        JdbcHelper.putVendor(getClass(), MariaDbDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}