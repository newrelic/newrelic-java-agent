/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import java.sql.ResultSetMetaData;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;

/**
 * Parses a sql string and returns a {@link ParsedDatabaseStatement}.
 * 
 * Insert, update, delete and select table statements are supported. For each of these, the model will be the table name
 * (the first table if the statement includes more than one table) and the operation will be the statement type (select,
 * insert, update or delete).
 * 
 * Some stored procedure statements are also supported. Create and drop procedure statements will be parsed as
 * Procedure/create and Procedure/drop - the name of the procedure is not included in the metric space since these calls
 * should be infrequent. Stored procedure calls ("call foo();") will be parsed so that the model is the name of the
 * procedure and the operation is 'call'.
 * 
 * This class is immutable and therefore thread safe.
 */
public interface DatabaseStatementParser {
    public static final String SELECT_OPERATION = "select";
    public static final String INSERT_OPERATION = "insert";

    /**
     * This is returned when parsing a statement in leu of returning null.
     */
    static final ParsedDatabaseStatement UNPARSEABLE_STATEMENT = new ParsedDatabaseStatement(null,
            DatastoreMetrics.DEFAULT_OPERATION, true);

    /**
     * Returns a parsed statement even if the statement is unparseable. Must not return null.
     * 
     *
     * @param databaseVendor
     * @param statement
     * @param resultSetMetaData
     */
    ParsedDatabaseStatement getParsedDatabaseStatement(DatabaseVendor databaseVendor, String statement,
            ResultSetMetaData resultSetMetaData);
}
