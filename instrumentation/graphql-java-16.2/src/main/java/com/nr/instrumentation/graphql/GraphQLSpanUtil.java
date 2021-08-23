package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.sun.corba.se.impl.orbutil.graph.Graph;
import graphql.*;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.List;

import static com.nr.instrumentation.graphql.GraphQLObfuscateUtil.obfuscateQuery;


public class GraphQLSpanUtil {

    public static void setOperationAttributes(Document document, String query){
        OperationDefinition definition = GraphQLOperationDefinition.firstFrom(document);
        // TODO: null handler from definition
        String operationName = definition.getName();
        String operationType = definition != null ? GraphQLOperationDefinition.getOperationTypeFrom(definition) : "Unavailable";
        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", operationType);
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", operationName != null ? operationName  : "<anonymous>");
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", obfuscateQuery(query));
    }

    public static void setResolverAttributes(ExecutionStrategyParameters parameters){
        AgentBridge.privateApi.addTracerParameter("graphql.field.path", parameters.getPath().getSegmentName());
        GraphQLObjectType type = (GraphQLObjectType) parameters.getExecutionStepInfo().getType();
        AgentBridge.privateApi.addTracerParameter("graphql.field.parentType", type.getName());
        AgentBridge.privateApi.addTracerParameter("graphql.field.name", parameters.getField().getName());
    }

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
}