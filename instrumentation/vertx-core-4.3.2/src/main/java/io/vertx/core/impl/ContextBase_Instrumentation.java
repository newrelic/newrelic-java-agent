/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl;

import com.newrelic.api.agent.weaver.Weave;

@Weave(originalName = "io.vertx.core.impl.ContextBase")
public abstract class ContextBase_Instrumentation {

    //static <T> Future<T> executeBlocking(ContextInternal context, Handler<Promise<T>> blockingCodeHandler, WorkerPool workerPool, TaskQueue queue)

//    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult> resultHandler,
//            Executor exec, TaskQueue queue, PoolMetrics metrics) {
//        VertxCoreUtil.storeToken(blockingCodeHandler);
//        VertxCoreUtil.storeToken(resultHandler);
//        Weaver.callOriginal();
//    }
//
//    @Trace(async = true)
//    private void lambda$executeBlocking$2(PoolMetrics metricsHandler, Object object, Handler handler, Handler resultHandler) {
//        VertxCoreUtil.linkAndExpireToken(handler);
//        Weaver.callOriginal();
//    }
//
//    @Trace(async = true)
//    private static void lambda$null$0(Handler handler, AsyncResult result, Void v) {
//        VertxCoreUtil.linkAndExpireToken(handler);
//        Weaver.callOriginal();
//    }

}
