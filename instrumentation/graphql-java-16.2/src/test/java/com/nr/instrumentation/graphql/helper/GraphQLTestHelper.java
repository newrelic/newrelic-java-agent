/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql.helper;

import graphql.language.Document;
import graphql.parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
