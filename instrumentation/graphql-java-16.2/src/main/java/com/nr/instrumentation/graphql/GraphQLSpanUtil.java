package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import graphql.*;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.FieldValueInfo;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static com.nr.instrumentation.graphql.GraphQLObfuscator.obfuscate;
import static com.nr.instrumentation.graphql.GraphQLOperationDefinition.getOperationTypeFrom;


public class GraphQLSpanUtil {

    private final static String DEFAULT_OPERATION_TYPE = "Unavailable";
    private final static String DEFAULT_OPERATION_NAME = "<anonymous>";

    public static void setOperationAttributes(final Document document, final String query){
        String nonNullQuery = getValueOrDefault(query, "");
        if(document == null) {
            setDefaultOperationAttributes(nonNullQuery);
            return;
        }
        OperationDefinition definition = GraphQLOperationDefinition.firstFrom(document);
        if(definition == null) {
            setDefaultOperationAttributes(nonNullQuery);
        }
        else {
            setOperationAttributes(getOperationTypeFrom(definition), definition.getName(), nonNullQuery);
        }
    }

    private static void setOperationAttributes(String type, String name, String query) {
        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", getValueOrDefault(type, DEFAULT_OPERATION_TYPE) );
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", getValueOrDefault(name, DEFAULT_OPERATION_NAME));
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", obfuscate(query));
    }

    private static void setDefaultOperationAttributes(String query) {;
        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", DEFAULT_OPERATION_TYPE);
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", DEFAULT_OPERATION_NAME);
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", query);
    }

    public static void setResolverAttributes(ExecutionStrategyParameters parameters){
        AgentBridge.privateApi.addTracerParameter("graphql.field.path", parameters.getPath().getSegmentName());
        GraphQLObjectType type = (GraphQLObjectType) parameters.getExecutionStepInfo().getType();
        AgentBridge.privateApi.addTracerParameter("graphql.field.parentType", type.getName());
        AgentBridge.privateApi.addTracerParameter("graphql.field.name", parameters.getField().getName());
    }

    public static void reportResolverThrowableToNR(Throwable e){
        NewRelic.noticeError(e);
    }

    public static void reportNonNullableExceptionToNR(FieldValueInfo result) {
        CompletableFuture<ExecutionResult> exceptionResult = result.getFieldValue();
        if (ifResultHasException(exceptionResult)) {
          reportExceptionFromCompletedExceptionally(exceptionResult);
        }
    }

    private static boolean ifResultHasException(CompletableFuture<ExecutionResult> exceptionResult){
        return exceptionResult != null && exceptionResult.isCompletedExceptionally();
    }

    private static void reportExceptionFromCompletedExceptionally(CompletableFuture<ExecutionResult> exceptionResult){
        try {
            exceptionResult.get();
        } catch (InterruptedException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Could not report GraphQL exception.");
        } catch (ExecutionException e) {
            NewRelic.noticeError(e.getCause());
        }
    }

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

    public static <T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}