/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package net.sourceforge.jtds.jdbcx;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.agent.bridge.datastore.JdbcDataSourceConnectionFactory;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import javax.sql.DataSource;
import java.sql.Connection;

@Weave(type = MatchType.BaseClass, originalName = "net.sourceforge.jtds.jdbcx.JtdsDataSource")
public abstract class JtdsDataSource_Instrumentation {

    // This is a leaf tracer because it's common for these methods to delegate to each other and we don't want double counts
    @Trace(leaf = true)
    public Connection getConnection() throws Exception {
        try {
            Connection connection = Weaver.callOriginal();
            AgentBridge.getAgent().getTracedMethod().addRollupMetricName(DatastoreMetrics.DATABASE_GET_CONNECTION);

            if (!JdbcHelper.connectionFactoryExists(connection)) {
                String url = JdbcHelper.getConnectionURL(connection);
                if (url == null) {
                    return connection;
                }

                // Detect correct vendor type and then store new connection factory based on URL
                DatabaseVendor vendor = JdbcHelper.getVendor(getClass(), url);
                JdbcHelper.putConnectionFactory(url, new JdbcDataSourceConnectionFactory(vendor, (DataSource) this));
            }

            return connection;
        } catch (Exception e) {
            AgentBridge.getAgent().getMetricAggregator().incrementCounter(DatastoreMetrics.DATABASE_ERRORS_ALL);
            throw e;
        }
    }

    // This is a leaf tracer because it's common for these methods to delegate to each other and we don't want double counts
    @Trace(leaf = true)
    public Connection getConnection(String username, String password) throws Exception {
        try {
            Connection connection = Weaver.callOriginal();
            NewRelic.getAgent().getTracedMethod().addRollupMetricName(DatastoreMetrics.DATABASE_GET_CONNECTION);

            if (!JdbcHelper.connectionFactoryExists(connection)) {
                String url = JdbcHelper.getConnectionURL(connection);
                if (url == null) {
                    return connection;
                }

                // Detect correct vendor type and then store new connection factory based on URL
                DatabaseVendor vendor = JdbcHelper.getVendor(getClass(), url);
                JdbcHelper.putConnectionFactory(url, new JdbcDataSourceConnectionFactory(vendor, (DataSource) this,
                        username, password));
            }
            return connection;
        } catch (Exception e) {
            AgentBridge.getAgent().getMetricAggregator().incrementCounter(DatastoreMetrics.DATABASE_ERRORS_ALL);
            throw e;
        }
    }
}
