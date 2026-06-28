/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl.future;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.CallbackWrapper;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Handler;

@Weave(originalName = "io.vertx.core.impl.future.FutureImpl")
public abstract class FutureImpl_Instrumentation {

    private Completable listener;

    public abstract boolean isComplete();

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void addListener(Completable listener) {
        Completable previousHandler = this.listener;
        if (isComplete()) {
            VertxCoreUtil.linkAndExpireToken(listener);
        }

        Weaver.callOriginal();

        if (!isComplete()) {
            if (previousHandler != this.listener) {
                VertxCoreUtil.storeToken(this.listener);
                if (previousHandler != null) {
                    VertxCoreUtil.expireToken(previousHandler);
                }
            }
        }
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    boolean completeInternal(Object result, Throwable cause) {
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }

    // Wrap success and failure handlers at registration time so they run in the correct
    // transaction context regardless of whether the callback is dispatched synchronously or
    // asynchronously. Both handlers share the same token so that whichever path runs (success
    // or failure) will expire it, preventing token leaks. This mirrors how 4.x used
    // AsyncHandlerWrapper for setResultHandler.
    @SuppressWarnings("unchecked")
    public Future onComplete(Handler successHandler, Handler failureHandler) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null && (successHandler != null || failureHandler != null)) {
            Token token = NewRelic.getAgent().getTransaction().getToken();
            if (successHandler != null) {
                successHandler = new CallbackWrapper<Object>(successHandler, token);
            }
            if (failureHandler != null) {
                failureHandler = new CallbackWrapper<Object>(failureHandler, token);
            }
        }
        return Weaver.callOriginal();
    }
}
