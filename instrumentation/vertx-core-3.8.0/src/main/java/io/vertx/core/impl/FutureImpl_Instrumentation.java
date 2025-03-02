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

    // see README for notes on this method
    @Trace(async = true, excludeFromTransactionTrace = true)
    public Future setHandler(Handler<AsyncResult> handler) {
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
