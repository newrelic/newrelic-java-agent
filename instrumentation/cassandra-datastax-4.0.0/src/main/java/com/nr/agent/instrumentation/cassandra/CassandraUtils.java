/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.session.Request;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.Segment;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
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
        String cassandraDialectPattern = String.join("|", Arrays.asList(SINGLE_QUOTE, COMMENT, MULTILINE_COMMENT, UUID, HEX, BOOLEAN, NUMBER));

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

    public static ResultSet wrapSyncRequest(Statement request, ResultSet result, CqlIdentifier keyspace, Segment segment) {
        if(result != null) {
            reportMetric(request, keyspace, result.getExecutionInfo().getCoordinator(), segment);
        }
        return result;
    }

    public static CompletionStage<AsyncResultSet> wrapAsyncRequest(Statement request, CompletionStage<AsyncResultSet> completionStage, CqlIdentifier keyspace, Segment segment) {
        return Objects.requireNonNull(completionStage).whenComplete(
                (result, throwable) -> {
                    if (throwable instanceof CompletionException) {
                        throwable = throwable.getCause();
                    }
                    if (throwable != null) {
                        System.out.println(throwable);
                        AgentBridge.privateApi.reportException(throwable);
                    }
                    if(result != null) {
                        reportMetric(request, keyspace, result.getExecutionInfo().getCoordinator(), segment);
                    }
                    segment.end();
                });
    }

    private static void reportMetric(Statement request, CqlIdentifier keyspace, Node coordinator, Segment segment)  {
        if(request instanceof BatchStatement) {
            CassandraUtils.metrics(null, null, "BATCH",
                    Optional.ofNullable(coordinator).flatMap(x -> x.getBroadcastAddress().map(InetSocketAddress::getHostName)).orElse(null),
                    Optional.ofNullable(coordinator).flatMap(x -> x.getBroadcastAddress().map(InetSocketAddress::getPort)).orElse(null),
                    Optional.ofNullable(keyspace).map(CqlIdentifier::asInternal).orElse(null),
                    AgentBridge.getAgent().getTransaction(),
                    segment);
        } else {
            CassandraUtils.metrics(
                    getQuery(request),
                    Optional.ofNullable(coordinator).flatMap(x -> x.getBroadcastAddress().map(InetSocketAddress::getHostName)).orElse(null),
                    Optional.ofNullable(coordinator).flatMap(x -> x.getBroadcastAddress().map(InetSocketAddress::getPort)).orElse(null),
                    Optional.ofNullable(keyspace).map(CqlIdentifier::asInternal).orElse(null),
                    AgentBridge.getAgent().getTransaction(),
                    segment
            );
        }
    }

    public static <RequestT extends Request> String getQuery(final RequestT statement) {
        String query = null;
        if (statement instanceof BoundStatement) {
            query = ((BoundStatement) statement).getPreparedStatement().getQuery();
        } else if (statement instanceof SimpleStatement) {
            query = ((SimpleStatement) statement).getQuery();
        }

        return query == null ? "" : query;
    }

}
