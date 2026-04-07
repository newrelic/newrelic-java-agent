/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl.future;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;

@Weave(originalName = "io.vertx.core.impl.future.FutureImpl")
abstract class FutureImpl_Instrumentation {

    private Listener listener;

    public abstract boolean isComplete();

    // The 1st time this method is called, this.listener will be null.
    // The provided listener will be assigned to this.listener and a token should be created for it.
    // The 2nd time, the original code will create a ListenerArray and put it in this.listener.
    // Then it will add both provided listeners to it.
    // At this point, the first token should be expired and a token should be created for the ListenerArray.
    // From there on, the ListenerArray will be used to store the listeners. And no token should be created.
    @Trace(async = true, excludeFromTransactionTrace = true)
    public void addListener(Listener listener) {
        Listener previousHandler = this.listener;
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
    public boolean tryComplete(Object result) {
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryFail(Throwable cause) {
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }
}
