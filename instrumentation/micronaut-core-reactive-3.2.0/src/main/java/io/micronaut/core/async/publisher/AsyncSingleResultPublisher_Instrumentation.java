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
import io.micronaut.core.async.subscriber.SingleThreadedBufferingSubscriber;
import org.reactivestreams.Subscriber;

@Weave(originalName = "io.micronaut.core.async.publisher.AsyncSingleResultPublisher", type = MatchType.ExactClass)
public abstract class AsyncSingleResultPublisher_Instrumentation<T> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void subscribe(Subscriber<? super T> subscriber) {
        if (!(subscriber instanceof NRSubscriber) && !(subscriber instanceof CompletableFuturePublisher) && !(subscriber instanceof SingleThreadedBufferingSubscriber)) {
            subscriber = new NRSubscriber(subscriber);
        }

        Weaver.callOriginal();
    }
}