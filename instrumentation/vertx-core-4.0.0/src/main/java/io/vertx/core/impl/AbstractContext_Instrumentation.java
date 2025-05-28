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
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

@Weave(originalName = "io.vertx.core.impl.AbstractContext")
abstract class AbstractContext_Instrumentation {
    private static <T> void setResultHandler(ContextInternal ctx, Future<T> fut, Handler<AsyncResult<T>> resultHandler) {
        if (resultHandler != null){
            Transaction txn = AgentBridge.getAgent().getTransaction(false);
            if (txn != null) {
                resultHandler = new AsyncHandlerWrapper<>(resultHandler, NewRelic.getAgent().getTransaction().getToken());
            }
        }
        Weaver.callOriginal();
    }
}
