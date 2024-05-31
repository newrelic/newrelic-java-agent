/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql.helper;

import com.newrelic.test.marker.Java8IncompatibleTest;
import graphql.language.Document;
import graphql.parser.Parser;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Category({ Java8IncompatibleTest.class })
public class GraphQLTestHelper {
    public static Document parseDocument(String testDir, String filename) {
        return Parser.parse(readText(testDir, filename));
    }

    public static String readText(String testDir, String filename) {
        try {
            String projectPath = String.format("src/test/resources/%s/%s.gql", testDir, filename);
            return new String(Files.readAllBytes(Paths.get(projectPath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Document parseDocumentFromText(String text) {
        return Parser.parse(text);
    }
}
