/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;


import java.sql.Connection;

public class DatastoreMetrics {

    // Datastore-related rollup metrics
    public static final String DATABASE_GET_CONNECTION = "Datastore/getConnection";
    public static final String DATABASE_ERRORS_ALL = "DatastoreErrors/all";

    /**
     * Notice a SQL statement to detect long running queries and enqueue for future processing.
     *
     * Note: This method has no implementation, instead it is automatically wired up in
     * {@link com.newrelic.agent.instrumentation.tracing.TraceClassVisitor} by creating a
     * SqlTracer (instead of a DefaultTracer) for the SQL specific functions.
     *
     * @param connection Connection used to run this query
     * @param sql The raw SQL string used in this query
     * @param params The parameters provided with the query (or null if no parameters are required)
     */
    public static void noticeSql(Connection connection, String sql, Object[] params) {
        // No-op
    }
}
