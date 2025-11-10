/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import java.sql.ResultSetMetaData;
import java.util.logging.Level;

/**
 * Cache a limited number of parsed database statements. <br>
 * This class is thread safe.
 */
public class CachingDatabaseStatementParser implements DatabaseStatementParser {

    // This class is thread safe post-v3.5.1 of the Agent. See JAVA-651.

    private final DatabaseStatementParser databaseStatementParser;
    private volatile Cache<String, ParsedDatabaseStatement> statements;

    public CachingDatabaseStatementParser(DatabaseStatementParser databaseStatementParser) {
        this.databaseStatementParser = databaseStatementParser;
    }

    private Cache<String, ParsedDatabaseStatement> getOrCreateCache() {
        // The infamous double-check locking pattern, which works correctly post-Java-1.5
        // so long as the shared instance variable ("statements") is declared volatile.
        if (null == statements) {
            synchronized (this) {
                if (null == statements) {
                    statements = Caffeine.newBuilder().maximumSize(1000).weakKeys().executor(Runnable::run).build();
                }
            }
        }
        return statements;
    }

    /**
     * Get parsed SQL, with caching.
     * 
     * @return Parsed statement or null if unable to parse the SQL.
     */
    @Override
    public ParsedDatabaseStatement getParsedDatabaseStatement(final DatabaseVendor databaseVendor, final String statement,
            final ResultSetMetaData resultSetMetaData) {

        Throwable toLog = null;

        try {
            if (statement == null) {
                Agent.LOG.log(Level.FINE, "Returning UNPARSEABLE_STATEMENT due to null statement for {0}", resultSetMetaData);
                return UNPARSEABLE_STATEMENT;
            }

            Cache<String, ParsedDatabaseStatement> cache = getOrCreateCache();
            ParsedDatabaseStatement parsedStatement = cache.getIfPresent(statement);
            if (parsedStatement == null) {
                parsedStatement = databaseStatementParser.getParsedDatabaseStatement(databaseVendor, statement, resultSetMetaData);
                cache.put(statement, parsedStatement);
            }

            return parsedStatement;
        } catch (RuntimeException ex) {
            toLog = ex;
        }

        Agent.LOG.log(Level.FINEST, "In cache.get() or its loader:", toLog);
        Agent.LOG.log(Level.FINE, "Returning UNPARSEABLE_STATEMENT for {0}", resultSetMetaData);
        return UNPARSEABLE_STATEMENT;
    }

}
