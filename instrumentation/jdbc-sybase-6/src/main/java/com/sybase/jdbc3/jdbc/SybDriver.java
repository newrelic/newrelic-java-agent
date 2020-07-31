/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.sybase.jdbc3.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.sybase.SybaseDatabaseVendor;

@Weave
public abstract class SybDriver {

    public Connection connect(String url, Properties props) {
        JdbcHelper.putVendor(getClass(), SybaseDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

} 
