/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.CallableWrapper;
import io.vertx.core.Future;

import java.util.concurrent.Callable;

@Weave(originalName = "io.vertx.core.impl.ContextImpl")
public abstract class ContextImpl_Instrumentation {

    public <T> Future<T> executeBlocking(Callable<T> blockingCodeHandler, boolean ordered) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            blockingCodeHandler =
                    new CallableWrapper<T>(blockingCodeHandler, NewRelic.getAgent().getTransaction().getToken());
        }
        return Weaver.callOriginal();
    }
}
