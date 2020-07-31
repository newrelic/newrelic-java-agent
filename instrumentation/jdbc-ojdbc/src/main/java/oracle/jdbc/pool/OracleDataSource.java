/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package oracle.jdbc.pool;

import java.sql.Connection;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.oracle.OracleDatabaseVendor;

@Weave(type = MatchType.BaseClass)
public abstract class OracleDataSource {

    public Connection getConnection(String userID, String pass) {
        JdbcHelper.putVendor(getClass(), OracleDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}
