/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.netflix.hystrix;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.helper.ExpireToken;
import com.nr.helper.GetToken;
import com.nr.helper.TokenHolder;
import rx.Observable;

/**
 * There are two command classes in Hystrix 1.4+, {@link HystrixObservableCommand} and {@link HystrixCommand}. They both
 * extend from this class, and use {@link #toObservable()} to turn the command into an rx.Observable. We hook that
 * method to generate an async token to link future work with, and appends an action to expire the token when the
 * observable is finished.
 *
 * @param <R>
 */
@Weave(originalName = "com.netflix.hystrix.AbstractCommand")
abstract class WeaveAbstractCommand<R> {

    @NewField
    protected TokenHolder commandToken;

    public abstract boolean isResponseTimedOut();

    @Trace(dispatcher = true)
    public Observable<R> toObservable() {
        Observable<R> output = Weaver.callOriginal();

        if (output.getClass().getName().equals("com.netflix.hystrix.AbstractCommand$CachedObservableResponse")) {
            NewRelic.getAgent().getTransaction().getTracedMethod().setMetricName("Java", getClass().getName(),
                    "toObservable", "ResultFromCache");
            // this is a cached result and so we do not want to create a token
            return output;
        }

        commandToken = new TokenHolder();

        // this ensures the token is expired no matter how this observable is run
        return output.doOnSubscribe(new GetToken(commandToken))
                     .doOnUnsubscribe(new ExpireToken(commandToken))
                     .finallyDo(new ExpireToken(commandToken));

    }

    @Trace(dispatcher = true)
    public abstract Observable<R> observe();
}
