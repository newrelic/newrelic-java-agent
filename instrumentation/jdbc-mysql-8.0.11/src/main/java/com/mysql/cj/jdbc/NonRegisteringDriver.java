/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mysql.cj.jdbc;

import java.sql.SQLException;
import java.util.Properties;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.mysql8011.MySQLDatabaseVendor;

@Weave(type = MatchType.BaseClass)
public abstract class NonRegisteringDriver {

    public java.sql.Connection connect(String url, Properties props) throws SQLException {
        JdbcHelper.putVendor(getClass(), MySQLDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}
