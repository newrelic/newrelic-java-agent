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

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void addListener(Listener listener) {
        if (isComplete()) {
            VertxCoreUtil.linkAndExpireToken(listener);
        } else {
            VertxCoreUtil.storeToken(listener);
        }
        Weaver.callOriginal();
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
