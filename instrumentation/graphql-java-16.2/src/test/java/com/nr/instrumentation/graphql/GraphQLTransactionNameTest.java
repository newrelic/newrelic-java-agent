package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.parser.Parser;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;


public class GraphQLTransactionNameTest {

    @Test
    public void testSimpleQuery() {
        //given
        Document document = parse("simpleQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/simple/libraries.books", transactionName);
    }

    @Test
    public void testSimpleAnonymousQuery() {
        //given
        Document document = parse("simpleAnonymousQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries.books", transactionName);
    }

    @Test
    public void testDeepestUniquePathQuery() {
        //given
        Document document = parse("deepestUniquePathQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries", transactionName);
    }

    @Test
    public void testDeepestUniqueSinglePathQuery() {
        //given
        Document document = parse("deepestUniqueSinglePathQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries.booksInStock.title", transactionName);
    }

    @Test
    public void testFederatedSubGraphQuery() {
        //given
        Document document = parse("federatedSubGraphQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries.branch", transactionName);
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
