/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import java.sql.Connection;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;

public interface SqlTracer extends SqlTracerExplainInfo, Tracer {

    // Tracer attributes
    String EXPLAIN_PLAN_PARAMETER_NAME = "explanation";
    String EXPLAIN_PLAN_FORMAT_PARAMETER_NAME = "explanation_format";
    String DATABASE_VENDOR_PARAMETER_NAME = "database_vendor";


    // Tracer, Transaction and TransactionSegment attributes
    String SQL_PARAMETER_NAME = "sql";
    String SQL_OBFUSCATED_PARAMETER_NAME = "sql_obfuscated";
    String SQL_HASH_VALUE = "sql_hash_value";

    /**
     * Returns a ConnectionFactory instance which can create a database connection on demand. This
     * will generally be used for running explain plans after a query has already executed.
     * 
     * @return ConnectionFactory instance
     */
    ConnectionFactory getConnectionFactory();

    /**
     * Store a connection factory instance on this SqlTracer for later use.
     * Note: A call to this method is automatically wired up in {@link com.newrelic.agent.instrumentation.tracing.NoticeSqlVisitor}
     * 
     * @param connectionFactory The ConnectionFactory instance to store
     */
    void setConnectionFactory(ConnectionFactory connectionFactory);

    /**
     * Returns the raw sql string that was used for a given query. This string does NOT include parameter values
     * when a PreparedStatement is used, just the raw sql passed in. However, if a Statement is crafted manually
     * this will return everything passed in.
     * 
     * @return The raw sql string
     */
    String getRawSql();

    /**
     * Grab the host and port from the connection and store it on this tracer.
     *
     * @param conn the connection instance to grab from
     */
    void provideConnection(Connection conn);

    /**
     * Store the raw sql string on this tracer.
     * Note: A call to this method is automatically wired up in {@link com.newrelic.agent.instrumentation.tracing.NoticeSqlVisitor}
     * 
     * @param rawSql the raw sql string to store
     */
    void setRawSql(String rawSql);

    /**
     * Returns an Object array containing the parameters (in order) that were set on a PreparedStatement. If the query
     * is using the Statement class or no parameters were passed in, this will return null.
     * 
     * @return Object array containing all parameter values in order, or null if none exist
     */
    Object[] getParams();

    /**
     * Store the parameters for this sql statement (must be in order).
     * Note: A call to this method is automatically wired up in {@link com.newrelic.agent.instrumentation.tracing.NoticeSqlVisitor}
     * 
     * @param params the parameters of this sql statement
     */
    void setParams(Object[] params);

    /**
     * Return the Transaction that this SqlTracer is participating in.
     * 
     * @return the current Transaction
     */
    Transaction getTransaction();

    /**
     * Returns the host of database instance used to run the query.
     *
     * @return host of database instance used to run the query.
     */
    String getHost();

    /**
     * Returns the port of database instance used to run the query.
     *
     * @return port of the database instance used to run the query.
     */
    Integer getPort();

    /**
     * If configured, a hash will be generated on the normalized SQL statement.
     * This method will return that hash value, or null if a hash hasn't beem
     * generated.
     *
     * @return the hash value of the normalized SQL statement or null
     * if a hash hasn't been generated
     */
    String getNormalizedSqlHashValue();
}
