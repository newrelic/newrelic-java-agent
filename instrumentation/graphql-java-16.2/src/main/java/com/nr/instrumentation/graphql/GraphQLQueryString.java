package com.nr.instrumentation.graphql;

import graphql.language.Document;

public class GraphQLQueryString {

    public static String from(Document document) {
        return "{book (id: ???) {title}}";
    }
}
