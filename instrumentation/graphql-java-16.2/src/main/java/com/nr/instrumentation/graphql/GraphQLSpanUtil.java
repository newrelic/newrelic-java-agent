package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import graphql.*;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.List;

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

    // TODO: Not used, can we remove this method?
    public static void maybeErrorOnResolver(List<GraphQLError> errors, String segmentName){
        errors.stream().filter(graphQLError -> matchSegmentFromPath(graphQLError, segmentName))
                .findFirst().ifPresent(GraphQLSpanUtil::reportGraphQLError);
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

    private static boolean matchSegmentFromPath(GraphQLError error, String segmentName) {
        List<Object> list = error.getPath();
        if(list != null) {
            String segment = (String) list.get(list.size()-1);
            return segment.equals(segmentName);
        } else return false;
    }

    public static <T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}