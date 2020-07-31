/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.newrelic.agent.database.DatabaseStatementParser.SELECT_OPERATION;
import static com.newrelic.agent.database.DefaultDatabaseStatementParser.PATTERN_SWITCHES;

class SelectVariableStatementFactory implements StatementFactory {
    private static final Pattern FROM_MATCHER = Pattern.compile("\\s+from\\s+", PATTERN_SWITCHES);

    private final ParsedDatabaseStatement innerSelectStatement = new ParsedDatabaseStatement("INNER_SELECT", SELECT_OPERATION, false);
    private final ParsedDatabaseStatement statement = new ParsedDatabaseStatement("VARIABLE", SELECT_OPERATION, false);
    // REVIEW not sure about this matcher
    private final Pattern pattern = Pattern.compile(".*select\\s+([^\\s,]*).*", PATTERN_SWITCHES);

    @Override
    public ParsedDatabaseStatement parseStatement(String statement) {
        Matcher matcher = pattern.matcher(statement);
        if (matcher.matches()) {
            if (FROM_MATCHER.matcher(statement).find()) {
                return innerSelectStatement;
            } else {
                return this.statement;
            }
        } else {
            return null;
        }
    }

    @Override
    public String getOperation() {
        return "select";
    }
}
