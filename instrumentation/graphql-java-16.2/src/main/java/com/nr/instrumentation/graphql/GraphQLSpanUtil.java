package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import graphql.*;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;

import static com.nr.instrumentation.graphql.GraphQLObfuscateUtil.obfuscateQuery;
import static com.nr.instrumentation.graphql.GraphQLTransactionName.getFirstOperationDefinitionFrom;
import static com.nr.instrumentation.graphql.GraphQLTransactionName.getOperationTypeFrom;


public class GraphQLSpanUtil {

    public static void setOperationAttributes(Document document, String query){
        OperationDefinition definition = getFirstOperationDefinitionFrom(document);
        String operationName = definition.getName();
        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", definition != null ? getOperationTypeFrom(definition) : "Unavailable");
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", operationName != null ? operationName  : "<anonymous>");
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", obfuscateQuery(query));
    }

    public static void setResolverAttributes(ExecutionStrategyParameters parameters){
        AgentBridge.privateApi.addTracerParameter("graphql.field.path", parameters.getPath().getSegmentName());
        GraphQLObjectType type = (GraphQLObjectType) parameters.getExecutionStepInfo().getType();
        AgentBridge.privateApi.addTracerParameter("graphql.field.parentType", type.getName());
        AgentBridge.privateApi.addTracerParameter("graphql.field.name", parameters.getField().getName());
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
}