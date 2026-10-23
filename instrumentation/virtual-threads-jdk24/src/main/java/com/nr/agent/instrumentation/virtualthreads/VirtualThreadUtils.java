/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.virtualthreads;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Transaction;

public class VirtualThreadUtils {

    public static final boolean isSupportedRuntime = isSupportedJavaVersion();

    public static Runnable getWrapper(Runnable runnable) {
        if (runnable == null || runnable instanceof VirtualThreadRunnable) {
            return null;
        }

        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (tx == null) {
            return null;
        }

        Token token = tx.getToken();
        if (!token.isActive()) {
            token.expire();
            return null;
        }

        return new VirtualThreadRunnable(runnable, token);
    }

    private static boolean isSupportedJavaVersion() {
        return isSupportedJavaVersion(System.getProperty("java.specification.version"));
    }

    static boolean isSupportedJavaVersion(String specVersion) {
        try {
            // Java 8 reports as "1.8"
            if (specVersion == null || specVersion.startsWith("1.")) {
                return false;
            }

            return Integer.parseInt(specVersion) >= 24;
        } catch (RuntimeException e) {
            return false;
        }
    }
}