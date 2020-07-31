/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.db2.jcc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.db2.Db2DatabaseVendor;

@Weave(type = MatchType.BaseClass)
public abstract class DB2Driver {

    public Connection connect(String url, Properties props) throws SQLException {
        JdbcHelper.putVendor(getClass(), Db2DatabaseVendor.INSTANCE);
        Connection connection = Weaver.callOriginal();
        if (connection != null && connection instanceof SQLJConnection && !JdbcHelper.databaseNameExists(connection)) {
            String databaseName = ((SQLJConnection) connection).getDatabaseName();
            if (databaseName != null) {
                JdbcHelper.putDatabaseName(url, databaseName);
            }
        }
        return connection;
    }

}
