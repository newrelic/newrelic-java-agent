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
import io.vertx.core.spi.metrics.PoolMetrics;

import java.util.concurrent.Executor;

@Weave(originalName = "io.vertx.core.impl.ContextImpl")
public class ContextImpl_Instrumentation {

    void executeBlocking(Action action, Handler<Future> blockingCodeHandler, Handler<AsyncResult> resultHandler,
            Executor exec, PoolMetrics metrics) {
        VertxCoreUtil.storeToken(blockingCodeHandler);
        VertxCoreUtil.storeToken(resultHandler);
        Weaver.callOriginal();
    }

    @Trace(async = true)
    private void lambda$executeBlocking$2(PoolMetrics metricsHandler, Object object, Handler handler, Action action,
            Handler resultHandler) {
        VertxCoreUtil.linkAndExpireToken(handler);
        Weaver.callOriginal();
    }
}
