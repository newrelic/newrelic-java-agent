/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql;

import com.newrelic.api.agent.NewRelic;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.GraphqlErrorException;
import graphql.execution.FieldValueInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class GraphQLErrorHandler {
    public static void reportNonNullableExceptionToNR(FieldValueInfo result) {
        CompletableFuture<ExecutionResult> exceptionResult = result.getFieldValue();
        if (resultHasException(exceptionResult)) {
            reportExceptionFromCompletedExceptionally(exceptionResult);
        }
    }

    public static void reportGraphQLException(GraphQLException exception) {
        NewRelic.noticeError(exception);
    }

    public static void reportGraphQLError(GraphQLError error) {
        NewRelic.noticeError(throwableFromGraphQLError(error));
    }

    private static boolean resultHasException(CompletableFuture<ExecutionResult> exceptionResult) {
        return exceptionResult != null && exceptionResult.isCompletedExceptionally();
    }

    private static void reportExceptionFromCompletedExceptionally(CompletableFuture<ExecutionResult> exceptionResult) {
        try {
            exceptionResult.get();
        } catch (InterruptedException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Could not report GraphQL exception.");
        } catch (ExecutionException e) {
            NewRelic.noticeError(e.getCause());
        }
    }

    private static Throwable throwableFromGraphQLError(GraphQLError error) {
        return GraphqlErrorException.newErrorException()
                .message(error.getMessage())
                .build();
    }
}
