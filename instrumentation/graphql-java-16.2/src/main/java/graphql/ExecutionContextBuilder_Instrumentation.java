/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package graphql;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.graphql.GraphQLTransactionName;
import graphql.execution.ExecutionContextBuilder;
import graphql.language.Document;

@Weave(originalName = "graphql.execution.ExecutionContextBuilder", type = MatchType.ExactClass)
public class ExecutionContextBuilder_Instrumentation {

    @Trace
    public ExecutionContextBuilder document(Document document) {
        String transactionName = GraphQLTransactionName.from(document);
        NewRelic.setTransactionName("GraphQL", transactionName);
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/operation" + transactionName);
        //fixme transactionname is "/rest of name", rollUp joins parts with delimiter of "/". String
        // ends up GraphQL/operation//rest of name.
        NewRelic.getAgent().getTracedMethod().addRollupMetricName("GraphQL/operation", transactionName);
        //todo running test() and debugging the introspector, you  can see the spans created from this tx. introspector.getSpanEvents()

        /*
        If the query was
        query fastAndFun {
            bookById (id: "book-1") {
                title
            }
        }

        These attributes and string values should be added

        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", query );
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", fastAndFun );
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", {book (???) {title}});

         */

        return Weaver.callOriginal();
    }
}
