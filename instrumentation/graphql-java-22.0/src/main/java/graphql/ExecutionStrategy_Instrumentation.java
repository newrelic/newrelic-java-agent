/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package graphql;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.FieldValueInfo;
import graphql.schema.DataFetchingEnvironment;

import java.util.concurrent.CompletableFuture;

import static com.nr.instrumentation.graphql.GraphQLErrorHandler.reportNonNullableExceptionToNR;
import static com.nr.instrumentation.graphql.GraphQLSpanUtil.setResolverAttributes;

@Weave(originalName = "graphql.execution.ExecutionStrategy", type = MatchType.BaseClass)
public class ExecutionStrategy_Instrumentation {

    @Trace
    protected Object resolveFieldWithInfo(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {

        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/resolve/" + parameters.getPath().getSegmentName());
        setResolverAttributes(parameters);
        return Weaver.callOriginal();
    }

    protected <T> CompletableFuture<T> handleFetchingException(DataFetchingEnvironment environment, ExecutionStrategyParameters parameters, Throwable e) {
        NewRelic.noticeError(e);
        return Weaver.callOriginal();
    }

    protected FieldValueInfo completeValue(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        FieldValueInfo result = Weaver.callOriginal();
        if (result != null) {
            reportNonNullableExceptionToNR(result);
        }
        return result;
    }
}
