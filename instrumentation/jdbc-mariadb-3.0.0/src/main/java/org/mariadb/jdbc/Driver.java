/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.mariadb.jdbc;

import java.util.Properties;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.mariadb.MariaDbDatabaseVendor;

@Weave
public abstract class Driver {

    public Connection connect(String url, Properties props) {
        JdbcHelper.putVendor(getClass(), MariaDbDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}
