package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GraphQLDocument {
    public static Document from(String testDir, String filename) {
        return Parser.parse(readText(testDir, filename));
    }

    private static String readText(String testDir, String filename) {
        try {
            String projectPath = String.format("src/test/resources/%s/%s.gql", testDir, filename);
            return new String(Files.readAllBytes(Paths.get(projectPath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
