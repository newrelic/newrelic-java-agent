package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.parser.Parser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphQLObfuscateQueryTest {

    private final static String OBFUSCATE_DATA_DIR = "obfuscateQueryTestData";

    private static Document parse(String filename) {
        return Parser.parse(readText(filename));
    }

    private static String readText(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/" + filename + ".gql")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/obfuscateQueryTestData/obfuscate-query-test-data.csv", delimiter = '|', numLinesToSkip = 2)
    public void testObfuscateQuery(String queryToObfuscateFile, String expectedObfuscatedQueryFile) {
        //setup
        queryToObfuscateFile = OBFUSCATE_DATA_DIR + "/" + queryToObfuscateFile.trim();
        expectedObfuscatedQueryFile = OBFUSCATE_DATA_DIR + "/" + expectedObfuscatedQueryFile.trim();
        String expectedObfuscatedResult = readText(expectedObfuscatedQueryFile);

        //given
        Document document = parse(queryToObfuscateFile);

        //when
        String obfuscatedQuery = GraphQLObfuscateUtil.getObfuscatedQuery(document);
        assertEquals(expectedObfuscatedResult, obfuscatedQuery);
    }
}
