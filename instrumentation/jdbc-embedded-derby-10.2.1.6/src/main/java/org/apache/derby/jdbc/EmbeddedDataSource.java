/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.derby.jdbc;

import java.sql.Connection;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.derby_10_6_1_0.DerbyDatabaseVendor;

@Weave(type = MatchType.BaseClass)
public abstract class EmbeddedDataSource {

    public Connection getConnection() {
        JdbcHelper.putVendor(getClass(), DerbyDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

    public Connection getConnection(String user, String password) {
        JdbcHelper.putVendor(getClass(), DerbyDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }
    
}
