/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InnerSelectStatementFactory implements StatementFactory {
    private final StatementFactory selectStatementFactory = new DefaultStatementFactory(DatabaseStatementParser.SELECT_OPERATION,
            DefaultDatabaseStatementParser.SELECT_PATTERN, true);

    private final Pattern innerSelectPattern = Pattern.compile("^\\s*SELECT.*?\\sFROM\\s*\\(\\s*(SELECT.*)",
            DefaultDatabaseStatementParser.PATTERN_SWITCHES);

    @Override
    public ParsedDatabaseStatement parseStatement(String statement) {
        String sql = statement;
        String res = null;
        while (true) {
            String res2 = findMatch(sql);
            if (res2 == null) {
                break;
            }
            res = res2;
            sql = res2;
        }

        if (res != null) {
            return selectStatementFactory.parseStatement(res);
        }
        return selectStatementFactory.parseStatement(statement);
    }

    private String findMatch(String statement) {
        Matcher matcher = innerSelectPattern.matcher(statement);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public String getOperation() {
        return "select";
    }
}
