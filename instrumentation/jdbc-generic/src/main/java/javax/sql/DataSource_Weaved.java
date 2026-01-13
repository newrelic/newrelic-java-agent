/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package javax.sql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.agent.bridge.datastore.JdbcDataSourceConnectionFactory;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.NewRelic;
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

            String url = JdbcHelper.getConnectionURL(connection);;
            DatabaseVendor vendor = JdbcHelper.getVendor(getClass(), url);
            if (!JdbcHelper.connectionFactoryExists(connection)) {

                if (url == null) {
                    return connection;
                }

                // Detect correct vendor type and then store new connection factory based on URL
                JdbcHelper.putConnectionFactory(url, new JdbcDataSourceConnectionFactory(vendor, (DataSource) this));
            }

            // TODO wrap in config check
            // TODO Get Vendor and pull vendor specific native metadata SQL
            String nativeMetadataConfig = System.getProperty("sql_metadata.native", "");
            if (!nativeMetadataConfig.isEmpty()) {
                System.out.println("SQL Native Metadata -- running query");
                try {
//                String sql = "SELECT now()";
//                try (PreparedStatement pstmt = connection.prepareStatement(sql);
//                     ResultSet rs = pstmt.executeQuery()) {
//                    if (rs.next()) {
//                        System.out.println("Current Time: " + rs.getTimestamp(1));
//                    }
//                }
                    try (CallableStatement stmt = connection.prepareCall("{call DBMS_APPLICATION_INFO.SET_MODULE(?, ?)}")) {
                        stmt.setString(1, NewRelic.getAgent().getConfig().getValue("app_name"));
                        stmt.setString(2, NewRelic.getAgent().getTransaction().getTransactionName());
                        stmt.execute();
                    }
                } catch (SQLException e) {
                    NewRelic.getAgent().getLogger().log(Level.FINEST, "Error occurred executing native metadata correlation statements: {0}" +
                            " SQL state: {1}  SQL error code: {2}", e.getMessage(), e.getSQLState(), e.getErrorCode());
                    System.out.println("SQL Native Metadata error: " + e.getMessage());
                }
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