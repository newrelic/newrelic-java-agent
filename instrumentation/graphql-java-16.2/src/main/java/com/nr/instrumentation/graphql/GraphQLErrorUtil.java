package com.nr.instrumentation.graphql;

import com.newrelic.api.agent.NewRelic;
import graphql.*;


public class GraphQLErrorUtil {

    public static void reportGraphQLException(GraphQLException exception){
        NewRelic.noticeError(exception);
    }

    public static void reportGraphQLError(GraphQLError error){
        NewRelic.noticeError(throwableFromGraphQLError(error));
    }

    private static Throwable throwableFromGraphQLError(GraphQLError error){
        return GraphqlErrorException.newErrorException()
                .message(error.getMessage())
                .build();
    }

}