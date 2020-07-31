/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class SqlObfuscationCrossAgentInput {
    
    private final String testName;
    private final String rawSql;
    private final Set<String> obfuscatedSql;
    private final Set<String> dialects;

    public SqlObfuscationCrossAgentInput(JSONObject input) {
        this.testName = (String) input.get("name");
        this.rawSql = (String) input.get("sql");
        this.obfuscatedSql = jsonArrayToSet((JSONArray) input.get("obfuscated"));
        this.dialects = jsonArrayToSet((JSONArray) input.get("dialects"));
    }

    public String getTestName() {
        return testName;
    }

    public String getRawSql() {
        return rawSql;
    }

    public Set<String> getObfuscatedSql() {
        return obfuscatedSql;
    }

    public Set<String> getDialects() {
        return dialects;
    }

    private Set<String> jsonArrayToSet(JSONArray input) {
        Set<String> result = new HashSet<>(input.size());
        for (Object value : input) {
            result.add(value.toString());
        }
        return result;
    }
    
}
