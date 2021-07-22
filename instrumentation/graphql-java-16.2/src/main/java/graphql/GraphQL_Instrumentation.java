/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package graphql;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.graphql.GraphQLMetricUtil;
import com.nr.instrumentation.graphql.GraphQLTransactionName;
import graphql.language.Document;
import graphql.parser.Parser;

import java.util.concurrent.CompletableFuture;

@Weave(originalName = "graphql.GraphQL", type = MatchType.ExactClass)
public class GraphQL_Instrumentation {

    @Trace
    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
//        System.out.println(executionInput.getContext().getClass().getCanonicalName());
//        GraphQL graphQL = null;
//        Document document = Parser.parse(executionInput.getQuery());
        System.out.println("Weaving execute() " + executionInput.getQuery());
        GraphQLTransactionName.from(executionInput.getQuery());
//        String transactionName = GraphQLTransactionName.from(document);
//        String operation = executionInput.getOperationName();
//        NewRelic.setTransactionName("GraphQL", transactionName);
//        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
//        GraphQLMetricUtil.metrics(tracedMethod, operation, executionInput);
//        GraphQLMetricUtil.metrics(tracedMethod, executionInput.getOperationName());
//        util.instrumentExecutionContext((ExecutionContext) executionInput.getContext());
//        CompletableFuture<ExecutionResult> cfResult = Weaver.callOriginal();
//        if(cfResult != null) {
//            cfResult.thenAccept(result -> util.instrumentExecutionResult(result, executionInput));
//        }
//        return cfResult;
        System.out.println("Done.");
        return Weaver.callOriginal();
    }

}
