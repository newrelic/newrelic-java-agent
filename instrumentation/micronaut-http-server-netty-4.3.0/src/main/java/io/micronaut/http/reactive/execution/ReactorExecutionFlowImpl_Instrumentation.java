/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.reactive.execution;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.micronaut.netty_43.NRBiConsumerWrapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.BiConsumer;

@Weave(originalName = "io.micronaut.http.reactive.execution.ReactorExecutionFlowImpl", type = MatchType.ExactClass)
abstract class ReactorExecutionFlowImpl_Instrumentation {

    @NewField
    protected Token token = null;

    <K> ReactorExecutionFlowImpl_Instrumentation(Publisher<K> value) {
    }

    <K> ReactorExecutionFlowImpl_Instrumentation(Mono<K> value) {
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Trace(async = true)
    public void onComplete(BiConsumer<? super Object, Throwable> fn) {
        if (token != null) {
            token.link();
            NRBiConsumerWrapper wrapper = new NRBiConsumerWrapper(fn, token);
            token = null;
            fn = wrapper;
        }
        Weaver.callOriginal();
    }
}
