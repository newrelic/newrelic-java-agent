/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl.future;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;

@Weave(originalName = "io.vertx.core.impl.future.FutureImpl")
public abstract class FutureImpl_Instrumentation {

    private Listener listener;

    public abstract boolean isComplete();

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void addListener(Listener listener) {
        if (isComplete()) {
//            VertxCoreUtil.debug(null, null, this.getClass().getName() + ".addListener/linkAndExpireToken");
            VertxCoreUtil.linkAndExpireToken(listener);
        } else {
            VertxCoreUtil.debug(null, null, this.getClass().getName() + ".addListener/storeToken");
            VertxCoreUtil.storeToken(listener);
        }
        Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryComplete(Object result) {
//        VertxCoreUtil.debug(null, null, this.getClass().getName() + ".tryComplete/linkAndExpireToken");
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryFail(Throwable cause) {
//        VertxCoreUtil.debug(null, null, this.getClass().getName() + ".tryFail/linkAndExpireToken");
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }
}
