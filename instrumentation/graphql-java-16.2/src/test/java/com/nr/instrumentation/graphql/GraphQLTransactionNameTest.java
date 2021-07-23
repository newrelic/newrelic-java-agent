package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.parser.Parser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;


public class GraphQLTransactionNameTest {

    @Test
    public void test() throws IOException {
        //given
        Document document = parse("simpleQuery");
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals("/QUERY/<anonymous>/libraries.books", transactionName);
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
