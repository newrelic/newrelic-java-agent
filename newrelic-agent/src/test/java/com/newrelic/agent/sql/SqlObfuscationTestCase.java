/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import java.util.Collections;
import java.util.Set;

public class SqlObfuscationTestCase {
    private final String testName;
    private final String rawSql;
    private final Set<String> obfuscatedSql;
    private final String dialect;

    public SqlObfuscationTestCase(String testName, String rawSql, Set<String> obfuscatedSql, String dialect) {
        this.testName = testName;
        this.rawSql = rawSql;
        this.obfuscatedSql = Collections.unmodifiableSet(obfuscatedSql);
        this.dialect = dialect;
    }

    @Override
    public String toString() {
        return testName + ":" + dialect;
    }

    public String getRawSql() {
        return rawSql;
    }

    public Set<String> getObfuscatedSql() {
        return obfuscatedSql;
    }

    public String getDialect() {
        return dialect;
    }
}
