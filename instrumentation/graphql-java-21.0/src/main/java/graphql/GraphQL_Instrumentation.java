/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package graphql;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.CompletableFuture;

@Weave(originalName = "graphql.GraphQL", type = MatchType.ExactClass)
public class GraphQL_Instrumentation {

    @Trace
    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return Weaver.callOriginal();
    }
}
