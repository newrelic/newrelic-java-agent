/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.micronaut.core.async.publisher;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.micronaut.NRSubscriber;
import org.reactivestreams.Subscriber;

@Weave(originalName = "io.micronaut.core.async.publisher.CompletableFuturePublisher", type = MatchType.ExactClass)
class CompletableFuturePublisher_Instrumentation<T> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void subscribe(Subscriber<? super T> subscriber) {
        if (!(subscriber instanceof NRSubscriber)) {
            subscriber = new NRSubscriber(subscriber);
        }

        Weaver.callOriginal();
    }
}