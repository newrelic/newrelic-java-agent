/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.netflix.hystrix;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.Future;

/**
 * There are two command classes in Hystrix 1.4+, {@link HystrixObservableCommand} and {@link HystrixCommand}. They both
 * serve roughly the same purpose. In this case we add some tracing and linking to support joining work during when
 * the command is run or the fallback is executed.
 *
 * @param <R>
 */
@Weave(type = MatchType.BaseClass, originalName = "com.netflix.hystrix.HystrixCommand")
public abstract class WeaveHystrixCommand<R> extends WeaveAbstractCommand<R> {

    @Trace(dispatcher = true)
    public abstract R execute();

    @Trace(dispatcher = true)
    public abstract Future<R> queue();

    @Trace(dispatcher = true)
    protected R run() {
        if (this.commandToken != null && this.commandToken.token != null) {
            this.commandToken.token.link();
        }
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    protected R getFallback() {

        if (this.commandToken != null && this.commandToken.token != null) {
            this.commandToken.token.link();
        }

        if (isResponseTimedOut()) {
            AgentBridge.privateApi.addCustomAttribute("TimedOut", "true");
        }

        return Weaver.callOriginal();
    }

}
