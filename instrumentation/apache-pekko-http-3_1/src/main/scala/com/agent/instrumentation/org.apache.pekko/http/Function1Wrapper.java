/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.runtime.AbstractFunction1;

public class Function1Wrapper<T, U> extends AbstractFunction1<T, U> {

    private final Function1<T, U> original;
    private final Token token;

    public Function1Wrapper(Function1<T, U> original, Token token) {
        this.original = original;
        this.token = token;
    }

    @Override
    @Trace(async = true)
    public U apply(T v1) {
        try {
            token.linkAndExpire();
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
        return original.apply(v1);
    }

}
