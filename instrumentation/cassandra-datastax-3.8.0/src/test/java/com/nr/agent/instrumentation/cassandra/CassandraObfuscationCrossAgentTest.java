/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Set;

import com.newrelic.api.agent.QueryConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;

public class CassandraObfuscationCrossAgentTest {

    @Test
    public void testSqlObfuscation() throws Exception {
        File file = getFile("sql_obfuscation.json");
        if (file == null) {
            Assert.fail("There were no files read in for testing.");
        } else {
            processFile(file);
        }
    }

    private void processFile(File current) throws Exception {
        System.out.println("Processing File: " + current);
        JSONArray tests = readJsonAndGetTests(current);
        for (Object currentTest : tests) {
            SqlObfuscationCrossAgentInput input = new SqlObfuscationCrossAgentInput((JSONObject) currentTest);
            if (!input.getDialects().contains("cassandra")) {
                continue; // Exclude all non-cassandra tests
            }
            System.out.println("Running: " + input.getTestName());
            runTest(input);
        }
    }

    private JSONArray readJsonAndGetTests(File file) throws Exception {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        JSONArray theTests = null;
        try {
            fr = new FileReader(file);
            theTests = (JSONArray) parser.parse(fr);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                }
            }
        }
        return theTests;
    }

    private void runTest(SqlObfuscationCrossAgentInput input) {
        String rawSql = input.getRawSql();
        Set<String> expectedObfuscatedSql = input.getObfuscatedSql();

        QueryConverter<String> cassandraQueryConverter = CassandraUtils.CASSANDRA_QUERY_CONVERTER;
        String actualObfuscatedSql = cassandraQueryConverter.toObfuscatedQueryString(rawSql);
        Assert.assertTrue("Expected: " + expectedObfuscatedSql + ", Actual: " + actualObfuscatedSql, expectedObfuscatedSql.contains(actualObfuscatedSql));
    }

    private static File getFile(String path) {
        String fullPath = getFullPath(path);
        File file = new File(fullPath);

        if (!file.exists()) {
            Assert.fail(path + " was expanded to " + fullPath + " which does not exist.");
        }

        return file;
    }

    private static String getFullPath(String partialPath) {
        if (!partialPath.startsWith("/")) {
            partialPath = '/' + partialPath;
        }
        URL resource = CassandraObfuscationCrossAgentTest.class.getResource(partialPath);

        if (resource == null) {
            return partialPath;
        }
        try {
            String path = URLDecoder.decode(resource.getPath(), "UTF-8");
            if (new File(path).exists()) {
                return path;
            }
        } catch (Exception ex) {
        }
        return resource.getPath();
    }
}
