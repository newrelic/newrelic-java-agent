/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.scheduler;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "reactor.core.scheduler.InstantPeriodicWorkerTask")
final class InstantPeriodicWorkerTask_Instrumentation {

    // We need to be able to link the Token here when executing on a supplied Scheduler
    // A Token should be available on the thread that this task executes on if tokenLift() was added to Hooks.onEachOperator
    @Trace(async = true, excludeFromTransactionTrace = true)
    public Void call() {
        return Weaver.callOriginal();
    }
}
