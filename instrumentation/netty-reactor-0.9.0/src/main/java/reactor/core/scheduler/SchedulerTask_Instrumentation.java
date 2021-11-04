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
import com.nr.instrumentation.reactor.netty.TokenLinkingSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Hooks_Instrumentation;

import static com.nr.instrumentation.reactor.netty.TokenLinkingSubscriber.tokenLift;

@Weave(originalName = "reactor.core.scheduler.SchedulerTask")
final class SchedulerTask_Instrumentation {

    // We need to be able to link the Token here when executing on a supplied Scheduler via Mono::publishOn
    @Trace(async = true, excludeFromTransactionTrace = true)
    public Void call() {
        // Add tokenLift hook if it hasn't already been added. This allows for tokens to be retrieved from
        // the current context and linked across threads at various points of the Flux/Mono lifecycle.
        if (!Hooks_Instrumentation.instrumented.getAndSet(true)) {
            Hooks.onEachOperator(TokenLinkingSubscriber.class.getName(), tokenLift());
        }

        return Weaver.callOriginal();
    }
}
