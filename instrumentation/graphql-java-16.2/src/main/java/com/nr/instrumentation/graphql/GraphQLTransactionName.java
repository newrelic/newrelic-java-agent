package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.parser.Parser;

public class GraphQLTransactionName {
    public static String from(Document document) {
        return null;
    }

    public static String from(String query) {
        //Document document = Parser.parse(query);
        System.out.println(query);
        return "";
    }
}
