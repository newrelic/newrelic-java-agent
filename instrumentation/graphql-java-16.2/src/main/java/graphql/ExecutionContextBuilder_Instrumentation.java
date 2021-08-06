/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.graphql.GraphQLTransactionName;
import graphql.execution.ExecutionContextBuilder;
import graphql.language.Document;
import graphql.language.OperationDefinition;

import static com.nr.instrumentation.graphql.GraphQLObfuscateUtil.getObfuscatedQuery;
import static com.nr.instrumentation.graphql.GraphQLTransactionName.*;

@Weave(originalName = "graphql.execution.ExecutionContextBuilder", type = MatchType.ExactClass)
public class ExecutionContextBuilder_Instrumentation {

    @Trace
    public ExecutionContextBuilder document(Document document) {
        String transactionName = GraphQLTransactionName.from(document);
        NewRelic.setTransactionName("GraphQL", transactionName);
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/operation" + transactionName);
        OperationDefinition definition = getFirstOperationDefinitionFrom(document);
        String operationName = definition.getName();
        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", definition != null ? getOperationTypeFrom(definition) : "Unavailable");
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", operationName != null ? operationName  : "<anonymous>");
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", getObfuscatedQuery(document));
        return Weaver.callOriginal();
    }
}
