/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.postgresql.ds.common;

import java.sql.Connection;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.postgresql_94_1208.PostgresDatabaseVendor;

@Weave(type = MatchType.BaseClass)
public abstract class BaseDataSource {

    @Trace
    public Connection getConnection(String userID, String pass) throws Exception {
        JdbcHelper.putVendor(getClass(), PostgresDatabaseVendor.INSTANCE);
        try {
            Connection connection = Weaver.callOriginal();
            AgentBridge.getAgent().getTracedMethod().addRollupMetricName(DatastoreMetrics.DATABASE_GET_CONNECTION);
            return connection;
        } catch (Exception e) {
            AgentBridge.getAgent().getMetricAggregator().incrementCounter(DatastoreMetrics.DATABASE_ERRORS_ALL);
            throw e;
        }
    }
}
