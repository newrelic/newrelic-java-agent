/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package net.sourceforge.jtds.jdbcx;

import java.sql.Connection;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.jtds.JtdsDatabaseVendor;

@Weave
public abstract class JtdsDataSource {

    public Connection getConnection() {
        JdbcHelper.putVendor(getClass(), JtdsDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

    public Connection getConnection(String user, String password) {
        JdbcHelper.putVendor(getClass(), JtdsDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}