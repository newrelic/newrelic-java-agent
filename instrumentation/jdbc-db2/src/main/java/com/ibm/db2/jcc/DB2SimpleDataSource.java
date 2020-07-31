/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.db2.jcc;

import java.sql.Connection;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.db2.Db2DatabaseVendor;

@Weave
public abstract class DB2SimpleDataSource {

    public Connection getConnection(String userID, String pass) {
        JdbcHelper.putVendor(getClass(), Db2DatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}
