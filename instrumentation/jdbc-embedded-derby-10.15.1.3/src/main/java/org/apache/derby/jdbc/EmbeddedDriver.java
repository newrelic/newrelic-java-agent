/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.derby.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.derby_10_15_1_3.DerbyDatabaseVendor;

@Weave
public abstract class EmbeddedDriver {

    public Connection connect(String url, Properties props) throws SQLException {
        JdbcHelper.putVendor(getClass(), DerbyDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}
