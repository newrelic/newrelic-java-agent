/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.scheduler;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import reactor.core.Disposable;
import reactor.util.annotation.Nullable;

@Weave(originalName = "reactor.core.scheduler.SchedulerTask")
final class SchedulerTask {

    @NewField
    private Token token;

    SchedulerTask(Runnable task, @Nullable Disposable parent) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (tx != null && tx.isStarted()) {
            if (token == null) {
                token = tx.getToken();
            }
        }
    }

    // We need to be able to link the Token here when executing on a supplied Scheduler via Mono::publishOn
    @Trace(async = true, excludeFromTransactionTrace = true)
    public Void call() {
        if(token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }
}
