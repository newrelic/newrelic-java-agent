/*
 *  Copyright 2021 New Relic Corporation. All rights reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package reactor.core.scheduler;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.TokenAwareRunnable;
import reactor.core.Disposable;

import java.util.concurrent.TimeUnit;

@Weave(type = MatchType.BaseClass, originalName = "reactor.core.scheduler.Schedulers")
public abstract class Schedulers_Instrumentation {

    /*
     * Using a TokenAwareRunnable to link the thread that creates these runnables and the thread that executes them.
     * This was previously done by linking token on flux/mono operations, but it looks like that was causing overhead.
     *
     * It is possible that during the processing of the flux/monos thread hops could occur and visibility would be lost.
     * In this case, custom instrumentation will be needed.
     *
     * When using Netty Reactor with SpringBoot the thread linking will be done by the instrumentation in
     * HttpTrafficHandler_Instrumentation.
     */
    @Weave(type = MatchType.ExactClass, originalName = "reactor.core.scheduler.Schedulers$CachedScheduler")
    static class CachedScheduler {
        final Scheduler cached = Weaver.callOriginal();

        public Disposable schedule(Runnable task) {

            return cached.schedule(new TokenAwareRunnable(task));
        }

        public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
            return cached.schedule(new TokenAwareRunnable(task), delay, unit);
        }
    }

}
