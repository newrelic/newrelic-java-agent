/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package javax.sql;

import java.sql.Connection;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.agent.bridge.datastore.JdbcDataSourceConnectionFactory;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * This interface match is here to properly record every time that a connection is requested from a data source.
 * Normally this could just live in each JDBC driver module, but this is generic enough that we want to capture it for
 * all JDBC drivers.
 * 
 * This instrumentation attempts to get the connection and if it's successful it will be returned and an unscoped metric
 * will be generated, otherwise we will record a metric indicating that an error occurred and re-throw the error.
 */
@Weave(originalName = "javax.sql.DataSource", type = MatchType.Interface)
public abstract class DataSource_Weaved {

    // This is a leaf tracer because it's common for these methods to delegate to each other and we don't want double
    // counts
    @Trace(leaf = true)
    public Connection getConnection() throws Exception {
        boolean firstInConnectPath = !DatastoreInstanceDetection.shouldDetectConnectionAddress();

        try {
            DatastoreInstanceDetection.detectConnectionAddress();
            Connection connection = Weaver.callOriginal();
            AgentBridge.getAgent().getTracedMethod().addRollupMetricName(DatastoreMetrics.DATABASE_GET_CONNECTION);

            DatastoreInstanceDetection.associateAddress(connection);

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
        } finally {
            if (firstInConnectPath) {
                DatastoreInstanceDetection.stopDetectingConnectionAddress();
            }
        }
    }

    // This is a leaf tracer because it's common for these methods to delegate to each other and we don't want double
    // counts
    @Trace(leaf = true)
    public Connection getConnection(String username, String password) throws Exception {
        boolean firstInConnectPath = !DatastoreInstanceDetection.shouldDetectConnectionAddress();
        try {

            DatastoreInstanceDetection.detectConnectionAddress();
            Connection connection = Weaver.callOriginal();
            AgentBridge.getAgent().getTracedMethod().addRollupMetricName(DatastoreMetrics.DATABASE_GET_CONNECTION);

            DatastoreInstanceDetection.associateAddress(connection);

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
        } finally {
            if (firstInConnectPath) {
                DatastoreInstanceDetection.stopDetectingConnectionAddress();
            }
        }
    }

}