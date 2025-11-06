/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLOutputType;

import static com.nr.instrumentation.graphql.GraphQLObfuscator.obfuscate;
import static com.nr.instrumentation.graphql.GraphQLOperationDefinition.getOperationTypeFrom;
import static com.nr.instrumentation.graphql.Utils.getValueOrDefault;

public class GraphQLSpanUtil {

    private final static String DEFAULT_OPERATION_TYPE = "Unavailable";
    private final static String DEFAULT_OPERATION_NAME = "<anonymous>";

    public static void setOperationAttributes(final Document document, final String query) {
        String nonNullQuery = getValueOrDefault(query, "");
        if (document == null) {
            setDefaultOperationAttributes(nonNullQuery);
            return;
        }
        OperationDefinition definition = GraphQLOperationDefinition.firstFrom(document);
        if (definition == null) {
            setDefaultOperationAttributes(nonNullQuery);
        } else {
            setOperationAttributes(getOperationTypeFrom(definition), definition.getName(), nonNullQuery);
        }
    }

    public static void setResolverAttributes(ExecutionStrategyParameters parameters) {
        AgentBridge.privateApi.addTracerParameter("graphql.field.path", parameters.getPath().getSegmentName());
        AgentBridge.privateApi.addTracerParameter("graphql.field.name", parameters.getField().getName());
        // ExecutionStepInfo is not nullable according to documentation
        GraphQLOutputType graphQLOutputType = parameters.getExecutionStepInfo().getType();
        setGraphQLFieldParentTypeIfPossible(graphQLOutputType);
    }

    private static void setGraphQLFieldParentTypeIfPossible(GraphQLOutputType graphQLOutputType) {
        // graphql.field.parentType is NOT required according to the Agent Spec
        if (graphQLOutputType instanceof GraphQLNamedSchemaElement) {
            GraphQLNamedSchemaElement named = (GraphQLNamedSchemaElement) graphQLOutputType;
            AgentBridge.privateApi.addTracerParameter("graphql.field.parentType", named.getName());
        }
    }

    private static void setOperationAttributes(String type, String name, String query) {
        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", getValueOrDefault(type, DEFAULT_OPERATION_TYPE));
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", getValueOrDefault(name, DEFAULT_OPERATION_NAME));
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", obfuscate(query));
    }

    private static void setDefaultOperationAttributes(String query) {
        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", DEFAULT_OPERATION_TYPE);
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", DEFAULT_OPERATION_NAME);
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", obfuscate(query));
    }
}