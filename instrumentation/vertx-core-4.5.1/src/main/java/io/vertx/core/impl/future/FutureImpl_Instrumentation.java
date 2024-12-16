/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl.future;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;

import java.util.ArrayList;

@Weave(originalName = "io.vertx.core.impl.future.FutureImpl")
public abstract class FutureImpl_Instrumentation {

    private Listener listener;

    public abstract boolean isComplete();

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void addListener(Listener listener) {
        boolean newListenerArray = false;
        if (isComplete()) {
            VertxCoreUtil.linkAndExpireToken(listener);
        } else {
            if (this.listener == null) {
                // storing a token for the first listener, which will be set in callOriginal
                VertxCoreUtil.storeToken(listener);
            } else if (!(this.listener instanceof ListenerArray)) {
                // when a 2nd listener is added, it will convert this.listener to a ListenerArray
                // so we expire the previous token, and after the ListenerArray is created, we store a token for that
                VertxCoreUtil.expireToken(this.listener);
                newListenerArray = true;
            }
        }

        Weaver.callOriginal();

        if (newListenerArray) {
            VertxCoreUtil.storeToken(this.listener);
        }
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryComplete(Object result) {
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryFail(Throwable cause) {
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.vertx.core.impl.future.FutureImpl$ListenerArray")
    private abstract static class ListenerArray<T> extends ArrayList<Listener<T>> implements Listener<T> {
    }
}