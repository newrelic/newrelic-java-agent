/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.inet.ora;

import java.sql.Connection;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.inet.oranxo.INetOracleDatabaseVendor;

@Weave(type = MatchType.BaseClass)
public abstract class OraDataSource {

    public Connection getConnection() {
        JdbcHelper.putVendor(getClass(), INetOracleDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

    public Connection getConnection(String user, String password) {
        JdbcHelper.putVendor(getClass(), INetOracleDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }
}