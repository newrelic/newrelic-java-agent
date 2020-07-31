/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.inet.tds;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.inet.merlia.MerliaDatabaseVendor;

@Weave
public abstract class TdsDriver {

    public Connection connect(String url, Properties props) {
        JdbcHelper.putVendor(getClass(), MerliaDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

} 
