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
import com.nr.vertx.instrumentation.PromiseHandlerWrapper;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

@Weave(originalName = "io.vertx.core.impl.ContextImpl")
abstract class ContextImpl_Instrumentation {
    static <T> Future<T> executeBlocking(ContextInternal context, Handler<Promise<T>> blockingCodeHandler, WorkerPool workerPool, TaskQueue queue) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            blockingCodeHandler = new PromiseHandlerWrapper<T>(blockingCodeHandler, NewRelic.getAgent().getTransaction().getToken());
        }
        return Weaver.callOriginal();
    }
}
