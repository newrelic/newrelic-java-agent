package com.nr.instrumentation.graphql;

import graphql.language.*;
import graphql.parser.Parser;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphQLTransactionNameTest {

    private final static String TEST_DATA_DIR = "transactionNameTestData";
    private final static String OBFUSCATE_DATA_DIR = "queryStringTestData";

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
    @CsvFileSource(resources = "/queryStringTestData/obfuscate-query-test-data.csv", delimiter = '|', numLinesToSkip = 2)
    public void testBuildQuery(String queryToObfuscateFile, String expectedObfuscatedQueryFile) {
        //setup
        queryToObfuscateFile = OBFUSCATE_DATA_DIR + "/" + queryToObfuscateFile.trim();
        expectedObfuscatedQueryFile = OBFUSCATE_DATA_DIR + "/" + expectedObfuscatedQueryFile.trim();
        String expectedObfuscatedResult = readText(expectedObfuscatedQueryFile);

        //given
        Document document = parse(queryToObfuscateFile);

        //when
        String obfuscatedQuery = GraphQLObfuscateHelper.obfuscate(document);
        assertEquals(expectedObfuscatedResult, obfuscatedQuery);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/transactionNameTestData/transaction-name-test-data.csv", delimiter = '|', numLinesToSkip = 2)
    public void testQuery(String testFileName, String expectedTransactionName) {
        //setup
        testFileName = TEST_DATA_DIR + "/" + testFileName.trim();
        expectedTransactionName = expectedTransactionName.trim();
        //given
        Document document = parse(testFileName);
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals(expectedTransactionName, transactionName);
    }
}
