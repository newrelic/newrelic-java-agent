/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.micronaut.core.async.subscriber;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.reactivestreams.Subscription;

@Weave(originalName = "io.micronaut.core.async.subscriber.SingleThreadedBufferingSubscriber", type = MatchType.BaseClass)
public abstract class SingleThreadedBufferingSubscriber_Instrumentation<T> {
    @NewField
    public Token token = null;

    public void onSubscribe(Subscription subscription) {
        if (token != null) {
            Token t = NewRelic.getAgent().getTransaction().getToken();
            if (t != null && t.isActive()) {
                token = t;
            } else if (t != null) {
                t.expire();
                t = null;
            }
        }

        Weaver.callOriginal();
    }

    @Trace(async = true)
    protected void doOnNext(T message) {
        if (token != null) {
            token.link();
        }

        Weaver.callOriginal();
    }

    @Trace(async = true)
    protected void doOnError(Throwable t) {
        NewRelic.noticeError(t);
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }

        Weaver.callOriginal();
    }

    @Trace(async = true)
    protected void doOnComplete() {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }

        Weaver.callOriginal();
    }
}