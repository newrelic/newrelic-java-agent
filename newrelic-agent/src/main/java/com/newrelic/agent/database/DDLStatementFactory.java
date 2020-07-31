/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import java.util.regex.Pattern;

class DDLStatementFactory extends DefaultStatementFactory {
    private final String type;

    public DDLStatementFactory(String key, Pattern pattern, String type) {
        super(key, pattern, false);
        this.type = type;
    }

    @Override
    ParsedDatabaseStatement createParsedDatabaseStatement(String model) {
        return new ParsedDatabaseStatement(type, key, isMetricGenerator());
    }
}
