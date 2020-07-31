/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.h2.jdbcx;

import java.sql.Connection;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.h2.H2DatabaseVendor;

@Weave
public abstract class JdbcDataSource {

    public Connection getConnection() {
        JdbcHelper.putVendor(getClass(), H2DatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

    public Connection getConnection(String user, String password) {
        JdbcHelper.putVendor(getClass(), H2DatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}
