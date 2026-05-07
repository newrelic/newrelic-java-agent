/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import java.util.function.BiConsumer;

public class NRBiConsumerTokenWrapper<R> implements BiConsumer<R, Throwable> {
    BiConsumer<R, Throwable> delegate = null;
    private static boolean isTransformed = false;


    public NRBiConsumerTokenWrapper(BiConsumer<R, Throwable> d) {
        delegate = d;
        if(!isTransformed) {
            isTransformed = true;
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
        }
    }

    @Override
    @Trace(dispatcher = true)
    public void accept(R r, Throwable throwable) {
        if(throwable != null) {
            NewRelic.noticeError(throwable);
        }
        if(delegate != null) {
            delegate.accept(r, throwable);
        }
    }
}