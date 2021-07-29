package com.nr.instrumentation.graphql;

import com.newrelic.api.agent.NewRelic;
import graphql.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GraphQLErrorHelper {

    //This prevents double reporting of the same error. Parse and validation errors are reported in separate instrumentation.
    public static void maybeReportExecutionResultError(CompletableFuture<ExecutionResult> executionResult) {
        try {
            List<GraphQLError> errors = executionResult.get().getErrors();
            if(!errors.isEmpty()){
                Optional<GraphQLError> error = errors.stream().filter(GraphQLErrorHelper::notSyntaxOrValidationError)
                        .findFirst();
                error.ifPresent(graphQLError -> NewRelic.noticeError(graphQLError.getMessage()));
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void reportGraphQLException(GraphQLException exception){
        NewRelic.noticeError(exception);
    }

    public static void reportGraphQLError(GraphQLError error){
        NewRelic.noticeError(throwableFromGraphQLError(error));
    }

    private static boolean notSyntaxOrValidationError(GraphQLError e) {
        String errorName = e.getClass().getSimpleName();
        return !errorName.equals("InvalidSyntaxError") && !errorName.equals(("ValidationError"));
    }

    private static Throwable throwableFromGraphQLError(GraphQLError error){
        return GraphqlErrorException.newErrorException()
                .message(error.getMessage())
                .build();
    }

}