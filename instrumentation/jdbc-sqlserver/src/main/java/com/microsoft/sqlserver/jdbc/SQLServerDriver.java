/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.microsoft.sqlserver.jdbc;

import java.sql.Connection;
import java.util.Properties;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.sqlserver.SqlServerDatabaseVendor;

@Weave
public abstract class SQLServerDriver {

    public Connection connect(String url, Properties props) {
        JdbcHelper.putVendor(getClass(), SqlServerDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}

