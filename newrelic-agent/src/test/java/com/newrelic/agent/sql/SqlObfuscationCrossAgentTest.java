/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.database.SqlObfuscator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@RunWith(Parameterized.class)
public class SqlObfuscationCrossAgentTest {

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection<SqlObfuscationTestCase> data() throws Exception {
        JSONArray result = CrossAgentInput.readJsonAndGetTests("com/newrelic/agent/cross_agent_tests/sql_obfuscation/sql_obfuscation.json");

        List<SqlObfuscationTestCase> tests = new LinkedList<>();
        for (Object currentTest : result) {
            SqlObfuscationCrossAgentInput inputAllDialects = new SqlObfuscationCrossAgentInput((JSONObject) currentTest);
            for (String dialect : inputAllDialects.getDialects()) {
                if (dialect.equals("cassandra")) {
                    // Cassandra obfuscation tests are in the cassandra-datastax instrumentation module
                    continue;
                }

                tests.add(new SqlObfuscationTestCase(
                        inputAllDialects.getTestName(),
                        inputAllDialects.getRawSql(),
                        inputAllDialects.getObfuscatedSql(),
                        dialect
                ));
            }
        }
        return tests;
    }

    @Parameterized.Parameter
    public SqlObfuscationTestCase input;

    @Test
    public void runTest() {
        String rawSql = input.getRawSql();
        Set<String> expectedObfuscatedSql = input.getObfuscatedSql();

        SqlObfuscator sqlObfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        String actualObfuscatedSql = sqlObfuscator.obfuscateSql(rawSql, input.getDialect());
        Assert.assertTrue(
                "Expected: " + expectedObfuscatedSql + ", Actual: " + actualObfuscatedSql,
                expectedObfuscatedSql.contains(actualObfuscatedSql)
        );
    }

}
