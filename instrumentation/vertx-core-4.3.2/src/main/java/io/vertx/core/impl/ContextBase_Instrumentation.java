/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.AsyncHandlerWrapper;
import com.nr.vertx.instrumentation.PromiseHandlerWrapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

@Weave(originalName = "io.vertx.core.impl.ContextBase")
public abstract class ContextBase_Instrumentation {
    static <T> Future<T> executeBlocking(ContextInternal context, Handler<Promise<T>> blockingCodeHandler, WorkerPool workerPool, TaskQueue queue) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            blockingCodeHandler = new PromiseHandlerWrapper<T>(blockingCodeHandler, NewRelic.getAgent().getTransaction().getToken());
        }
        return Weaver.callOriginal();
    }

    static <T> void setResultHandler(ContextInternal ctx, Future<T> fut, Handler<AsyncResult<T>> resultHandler) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            resultHandler = new AsyncHandlerWrapper<>(resultHandler, NewRelic.getAgent().getTransaction().getToken());
        }
        Weaver.callOriginal();
    }
}
