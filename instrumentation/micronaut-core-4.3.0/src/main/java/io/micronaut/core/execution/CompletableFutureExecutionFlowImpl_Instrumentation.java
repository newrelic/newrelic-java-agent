/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.core.execution;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.micronaut.NRBiConsumerWrapper;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

@Weave(originalName = "io.micronaut.core.execution.CompletableFutureExecutionFlowImpl", type = MatchType.ExactClass)
class CompletableFutureExecutionFlowImpl_Instrumentation {
    @NewField
    protected Token token = null;

    CompletableFutureExecutionFlowImpl_Instrumentation(CompletionStage<Object> stage) {
        Token t = NewRelic.getAgent().getTransaction().getToken();
        if (t != null) {
            token = t;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Trace(async = true)
    public void onComplete(BiConsumer<? super Object, Throwable> throwable) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
            throwable = new NRBiConsumerWrapper<>(throwable);
        }

        Weaver.callOriginal();
    }
}