/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.netty.reactive;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "io.micronaut.http.netty.reactive.HandlerSubscriber", type = MatchType.ExactClass)
public abstract class HandlerSubscriber_Instrumentation<T> {

    @Trace(dispatcher = true)
    public void onComplete() {
        Weaver.callOriginal();
    }

    public void onError(final Throwable error) {
        NewRelic.noticeError(error);
        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public void onNext(T t) {
        Weaver.callOriginal();
    }
}
