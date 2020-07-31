/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import com.google.common.base.Joiner;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.Segment;

import java.util.logging.Level;
import java.util.regex.Pattern;

public class CassandraUtils {

    private static final String SINGLE_QUOTE = "'(?:[^']|'')*?(?:\\\\'.*|'(?!'))";
    private static final String COMMENT = "(?:#|--).*?(?=\\r|\\n|$)";
    private static final String MULTILINE_COMMENT = "/\\*(?:[^/]|/[^*])*?(?:\\*/|/\\*.*)";
    private static final String UUID = "\\{?(?:[0-9a-f]\\-*){32}\\}?";
    private static final String HEX = "0x[0-9a-f]+";
    private static final String BOOLEAN = "\\b(?:true|false|null)\\b";
    private static final String NUMBER = "-?\\b(?:[0-9]+\\.)?[0-9]+([eE][+-]?[0-9]+)?";

    private static final Pattern CASSANDRA_DIALECT_PATTERN;
    private static final Pattern CASSANDRA_UNMATCHED_PATTERN;
    private static final CQLParser CASSANDRA_QUERY_PARSER = new CQLParser();

    static {
        String cassandraDialectPattern = Joiner.on("|").join(SINGLE_QUOTE, COMMENT, MULTILINE_COMMENT,
                UUID, HEX, BOOLEAN, NUMBER);

        CASSANDRA_DIALECT_PATTERN = Pattern.compile(cassandraDialectPattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        CASSANDRA_UNMATCHED_PATTERN = Pattern.compile("'|/\\*|\\*/", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    }

    public static void metrics(String queryString, String host, Integer port, String keyspace, Transaction tx,
            Segment segment) {
        try {
            CQLParser.OperationAndTableName result = CASSANDRA_QUERY_PARSER.getOperationAndTableName(queryString);
            if (result == null) {
                NewRelic.getAgent().getLogger().log(Level.FINE, "Unable to parse cql statement");
                return;
            }

            CassandraUtils.metrics(queryString, result.tableName, result.operation, host, port, keyspace, tx, segment);
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "ERROR: Problem parsing cql statement. {0}", e);
        }
    }

    public static void metrics(String queryString, String collection, String operation, String host, Integer port,
            String keyspace, Transaction tx, Segment segment) {

        segment.reportAsExternal(DatastoreParameters
                .product(DatastoreVendor.Cassandra.name())
                .collection(collection)
                .operation(operation)
                .instance(host, port)
                .databaseName(keyspace) // may be null, indicating no keyspace for the command
                .slowQuery(queryString, CASSANDRA_QUERY_CONVERTER)
                .build());
    }

    public static QueryConverter<String> CASSANDRA_QUERY_CONVERTER = new QueryConverter<String>() {

        @Override
        public String toRawQueryString(String statement) {
            return statement;
        }

        @Override
        public String toObfuscatedQueryString(String statement) {
            return obfuscateQuery(statement);
        }

        private String obfuscateQuery(String rawQuery) {
            String obfuscatedSql = CASSANDRA_DIALECT_PATTERN.matcher(rawQuery).replaceAll("?");
            return checkForUnmatchedPairs(CASSANDRA_UNMATCHED_PATTERN, obfuscatedSql);
        }

        /**
         * This method will check to see if there are any open single quotes or comment open/closes still left in the
         * obfuscated string. If so, it means something didn't obfuscate properly so we will return "?"  instead to
         * prevent any data from leaking.
         */
        private String checkForUnmatchedPairs(Pattern pattern, String obfuscatedSql) {
            return pattern.matcher(obfuscatedSql).find() ? "?" : obfuscatedSql;
        }
    };

}
