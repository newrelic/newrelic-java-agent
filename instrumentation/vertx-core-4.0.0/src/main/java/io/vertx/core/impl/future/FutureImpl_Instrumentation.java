/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl.future;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

@Weave(originalName = "io.vertx.core.impl.future.FutureImpl")
abstract class FutureImpl_Instrumentation {

    @NewField
    private Handler handler = null;
    private Listener listener;

    public abstract boolean isComplete();

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void addListener(Listener listener) {
        if (isComplete()) {
            VertxCoreUtil.linkAndExpireToken(listener);
        } else {
            VertxCoreUtil.storeToken(listener);
        }
//        if (!isComplete()) {
//            System.out.println("addListener " + "  " + this.listener + "    " + listener + "   " + System.identityHashCode(listener) + "  " + System.currentTimeMillis());
//            assignAndStoreHandlerInstance(listener);
//        }
        Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public Future onSuccess(Handler handler) {
        System.out.println("success");
        //assignAndStoreHandlerInstance(handler);
        return Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public Future onComplete(Handler<AsyncResult> handler) {
        System.out.println("complete");
        //assignAndStoreHandlerInstance(handler);
        return Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public Future onFailure(Handler<AsyncResult> handler) {
        //assignAndStoreHandlerInstance(handler);
        return Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryComplete(Object result) {
        System.out.println("trycomplete "  + "  " + listener + "   " + System.identityHashCode(this) + "  " + System.currentTimeMillis());
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryFail(Throwable cause) {
        System.out.println("tryFail");
        VertxCoreUtil.linkAndExpireToken(this.listener);
        return Weaver.callOriginal();
    }

    private void assignAndStoreHandlerInstance(Listener listener) {
        VertxCoreUtil.storeToken(listener);
    }

//    private void assignAndStoreHandlerInstance(Handler listener) {
//        VertxCoreUtil.storeToken(listener);
//    }

}
