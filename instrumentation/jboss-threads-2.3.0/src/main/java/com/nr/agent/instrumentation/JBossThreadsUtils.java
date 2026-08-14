/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import java.util.concurrent.RunnableFuture;

public class JBossThreadsUtils {
    private static final String NEWRELIC_AGENT_PACKAGE = "com.newrelic.agent";

    public static <T> NRRunnable getWrapper(Runnable runnable) {
        if (runnable == null || runnable instanceof NRRunnable || runnable instanceof RunnableFuture) {
            return null;
        }

        Package runPackage = runnable.getClass().getPackage();
        if (runPackage != null && runPackage.getName().startsWith(NEWRELIC_AGENT_PACKAGE)) {
            return null;
        }

        Token token = NewRelic.getAgent().getTransaction().getToken();
        if (!token.isActive()) {
            token.expire();
            return null;
        }

        if (runnable instanceof Comparable) {
            return new NRComparableRunnable<T>(runnable, token);
        }

        return new NRRunnable(runnable, token);
    }
}
