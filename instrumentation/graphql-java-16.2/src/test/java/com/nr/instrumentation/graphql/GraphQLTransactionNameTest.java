package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.parser.Parser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphQLTransactionNameTest {

    private final static String TEST_DATA_DIR = "transactionNameTestData";

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

    @Test
    public void testSimpleAnonymousQuery() {
        //given
        Document document = parse("transactionNameTestData/simpleAnonymousQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries.books", transactionName);
    }

    @Test
    public void testDeepestUniquePathQuery() {
        //given
        Document document = parse("transactionNameTestData/deepestUniquePathQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries", transactionName);
    }

    @Test
    public void testDeepestUniqueSinglePathQuery() {
        //given
        Document document = parse("transactionNameTestData/deepestUniqueSinglePathQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries.booksInStock.title", transactionName);
    }

    @Test
    public void testFederatedSubGraphQuery() {
        //given
        Document document = parse("transactionNameTestData/federatedSubGraphQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries.branch", transactionName);
    }

    @Disabled // TODO: needs implementation with better handling of fragments
    @Test
    public void testUnionTypesAndInlineFragmentQuery() {
        //given
        Document document = parse("transactionNameTestData/unionTypesAndInlineFragmentQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/example/search<Author>.name", transactionName);
    }

    @Test
    public void testUnionTypesAndInlineFragmentsQuery() {
        //given
        Document document = parse("transactionNameTestData/unionTypesAndInlineFragmentsQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/example/search", transactionName);
    }

    @Test
    public void testValidationErrors_ShouldShowNameSame() {
        //given
        Document document = parse("transactionNameTestData/validationErrors");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/GetBooksByLibrary/libraries.books.doesnotexist.name", transactionName);
    }

    @Disabled // TODO: probably handle at a different level
    @Test
    public void testParsingErrors() {
        //given
        Document document = parse("transactionNameTestData/parsingErrors");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/*", transactionName);
    }

    @Disabled // TODO: not sure Java GraphQL supports batch queries based on parsing errors
    @Test
    public void testBatchQueries() {
        //given
        Document document = parse("transactionNameTestData/batchQueries");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/batch/query/GetBookForLibrary/library.books/mutation/<anonymous>/addThing", transactionName);
    }

    private static Document parse(String filename) {
        return Parser.parse(readText(filename));
    }

    private static String readText(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/" + filename)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
