/*
 *  Copyright 2021 New Relic Corporation. All rights reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package reactor.core.scheduler;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.TokenLinkingSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Hooks_Instrumentation;

import static com.nr.instrumentation.reactor.TokenLinkingSubscriber.tokenLift;

@Weave(type = MatchType.BaseClass, originalName = "reactor.core.scheduler.Schedulers")
public abstract class Schedulers_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "reactor.core.scheduler.Schedulers$CachedScheduler")
    static class CachedScheduler {
        CachedScheduler(String key, Scheduler cached) {
            /*
             * Add tokenLift hook if it hasn't already been added. This allows for tokens to be retrieved from
             * the current context and linked across threads at various points of the Flux/Mono lifecycle.
             *
             * When using Netty Reactor with SpringBoot this hook will be added by the HttpTrafficHandler_Instrumentation
             * but when using other embedded web servers (e.g. Tomcat, Jetty, Undertow) the HttpTrafficHandler class
             * doesn't get loaded and thus the hook isn't added. This ensures that the hook is added in a common code
             * path before any Scheduler Tasks are spun off on new threads.
             */
            if (!Hooks_Instrumentation.instrumented.getAndSet(true)) {
                Hooks.onEachOperator(TokenLinkingSubscriber.class.getName(), tokenLift());
            }
        }
    }

}
