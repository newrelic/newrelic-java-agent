/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

@Weave(originalName = "io.vertx.core.impl.FutureImpl")
abstract class FutureImpl_Instrumentation {

    private Handler<AsyncResult> handler = Weaver.callOriginal();

    public abstract boolean isComplete();

    // The 1st time this method is called, this.handler will be null.
    // The provided handler will be assigned to this.handler and a token should be created for it.
    // The 2nd time, the original code will create a Handlers and put it in this.handler.
    // Then it will add both provided handlers to it.
    // At this point, the first token should be expired and a token should be created for the Handlers.
    // From there on, the Handlers will be used to store the handlers. And no token should be created.
    @Trace(async = true, excludeFromTransactionTrace = true)
    public Future onComplete(Handler<AsyncResult> handler) {
        Handler previousHandler = this.handler;
        if (isComplete()) {
            VertxCoreUtil.linkAndExpireToken(handler);
        }

        Future future = Weaver.callOriginal();

        if (!isComplete()) {
            if (previousHandler != this.handler) {
                VertxCoreUtil.storeToken(this.handler);
                if (previousHandler != null) {
                    VertxCoreUtil.expireToken(previousHandler);
                }
            }
        }
        return future;
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryComplete(Object result) {
        VertxCoreUtil.linkAndExpireToken(handler);
        return Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryFail(Throwable cause) {
        VertxCoreUtil.linkAndExpireToken(handler);
        return Weaver.callOriginal();
    }
}