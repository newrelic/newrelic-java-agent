/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import com.newrelic.api.agent.NewRelic;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simplistic CQL "Parser" that attempts to extract the information we care about for Datastore requests:
 * <p>
 * - Operation (SELECT, INSERT, UPDATE, etc)
 * - Table Name (Column Family)
 */
public class CQLParser {

    private static final String IDENTIFIER_REGEX = "[a-zA-Z][a-zA-Z0-9_\\.]*";
    private static final int FLAGS = Pattern.DOTALL | Pattern.CASE_INSENSITIVE;

    private static final Pattern SELECT_PATTERN = Pattern.compile("^(SELECT(?:\\s+JSON)?)\\s+.+?FROM\\s+(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern INSERT_PATTERN = Pattern.compile("^(INSERT)\\s+INTO\\s+(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^(UPDATE)\\s+(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern DELETE_PATTERN = Pattern.compile("^(DELETE).+?FROM\\s+(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern BATCH_PATTERN = Pattern.compile("^BEGIN\\s+(?:(?:UNLOGGED|COUNTER)\\s+)?(BATCH)", FLAGS);
    private static final Pattern USE_PATTERN = Pattern.compile("^(USE)\\s+(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern KEYSPACE_PATTERN = Pattern.compile("^([A-Za-z]+\\s+KEYSPACE)\\s+(?:IF\\s+(?:NOT\\s+)?EXISTS\\s+)?(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern TABLE_PATTERN = Pattern.compile("^([A-Za-z]+\\s+(?:TABLE|COLUMNFAMILY))\\s+(?:IF\\s+(?:NOT\\s+)?EXISTS\\s+)?(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern TRUNCATE_PATTERN = Pattern.compile("^(TRUNCATE)\\s+(?:(?:TABLE|COLUMNFAMILY)\\s+)?(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile("^(CREATE\\s+(?:CUSTOM\\s+)?INDEX).+?ON\\s+(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern DROP_INDEX_PATTERN = Pattern.compile("^(DROP\\s+INDEX)\\s+(?:IF EXISTS\\s+)?(?:'|\")?(.*)", FLAGS);
    private static final Pattern TYPE_PATTERN = Pattern.compile("^([A-Za-z]+\\s+TYPE)\\s+(?:IF\\s+(?:NOT\\s+)?EXISTS\\s+)?(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern TRIGGER_PATTERN = Pattern.compile("^([A-Za-z]+\\s+TRIGGER)\\s+.*?ON\\s+(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern CREATE_FUNCTION_PATTERN = Pattern.compile("^(CREATE\\s+(?:OR\\s+REPLACE\\s+)?FUNCTION)\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(.+?)\\s", FLAGS);
    private static final Pattern DROP_FUNCTION_PATTERN = Pattern.compile("^(DROP\\s+FUNCTION)\\s+(?:IF\\s+EXISTS\\s+)?(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern CREATE_AGGREGATE_PATTERN = Pattern.compile("^(CREATE\\s+(?:OR\\s+REPLACE\\s+)?AGGREGATE)\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final Pattern DROP_AGGREGATE_PATTERN = Pattern.compile("^(DROP\\s+AGGREGATE)\\s+(?:IF\\s+EXISTS\\s+)?(?:'|\")?(" + IDENTIFIER_REGEX + ")", FLAGS);
    private static final String COMMENT_PATTERN = "/\\*(?:.|[\\r\\n])*?\\*/";

    private static final List<Pattern> PATTERNS = new LinkedList<>();

    static {
        // The order here is a performance optimization to favor more common queries first
        PATTERNS.add(SELECT_PATTERN);
        PATTERNS.add(UPDATE_PATTERN);
        PATTERNS.add(INSERT_PATTERN);
        PATTERNS.add(DELETE_PATTERN);
        PATTERNS.add(BATCH_PATTERN);
        PATTERNS.add(TRUNCATE_PATTERN); // This needs to be before TABLE_PATTERN
        PATTERNS.add(TABLE_PATTERN);
        PATTERNS.add(KEYSPACE_PATTERN);
        PATTERNS.add(USE_PATTERN);
        PATTERNS.add(TYPE_PATTERN);
        PATTERNS.add(CREATE_INDEX_PATTERN);
        PATTERNS.add(DROP_INDEX_PATTERN);
        PATTERNS.add(CREATE_FUNCTION_PATTERN);
        PATTERNS.add(DROP_FUNCTION_PATTERN);
        PATTERNS.add(CREATE_AGGREGATE_PATTERN);
        PATTERNS.add(DROP_AGGREGATE_PATTERN);
        PATTERNS.add(TRIGGER_PATTERN);
    }

    public OperationAndTableName getOperationAndTableName(String rawQuery) {
        try {
            rawQuery = rawQuery.replaceAll(COMMENT_PATTERN, "").trim();

            String operation = null;
            String tableName = null;
            for (Pattern pattern : PATTERNS) {
                Matcher matcher = pattern.matcher(rawQuery);
                if (matcher.find()) {
                    if (matcher.groupCount() >= 1) {
                        operation = matcher.group(1);
                    }
                    if (matcher.groupCount() == 2) {
                        tableName = matcher.group(2);
                    }
                    return new OperationAndTableName(operation, tableName);
                }
            }
        } catch (Exception ex) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Exception getting operation and table name");
            return null;
        }
        return null;
    }

    public class OperationAndTableName {
        public final String operation;
        public final String tableName;

        public OperationAndTableName(String operation, String tableName) {
            this.operation = operation.toUpperCase().replaceAll("\\s", "_");
            if (tableName != null) {
                tableName = tableName.replaceAll(";|'|\"", "");
            }
            this.tableName = tableName;
        }
    }
}
