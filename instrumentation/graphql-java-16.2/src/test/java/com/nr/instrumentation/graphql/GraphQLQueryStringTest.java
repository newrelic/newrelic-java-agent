package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphQLQueryStringTest {

    private final static String TEST_DATA_DIR = "queryStringTestData";

    @Test
    public void testQuery() {
        //given
        Document document = GraphQLDocument.from(TEST_DATA_DIR, "fastAndFun");
        //when
        String queryString = GraphQLQueryString.from(document);
        //then
        assertEquals("{book (id: ???) {title}}", queryString);
    }
}
