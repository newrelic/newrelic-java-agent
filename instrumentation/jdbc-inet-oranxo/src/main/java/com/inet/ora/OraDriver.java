/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.inet.ora;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.inet.oranxo.INetOracleDatabaseVendor;

@Weave
public abstract class OraDriver {

    public Connection connect(String url, Properties props) {
        JdbcHelper.putVendor(getClass(), INetOracleDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

} 
