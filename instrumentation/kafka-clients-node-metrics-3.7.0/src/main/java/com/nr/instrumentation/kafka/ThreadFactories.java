/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactories {
    public static ThreadFactory build(final String serviceName) {
        return new DefaultThreadFactory(serviceName, true);
    }

    private ThreadFactories() {
        // prevents instantiation
    }

    private static class DefaultThreadFactory implements ThreadFactory {
        private final String serviceName;
        private final AtomicInteger counter;
        private final boolean daemon;

        private DefaultThreadFactory(String serviceName, boolean daemon) {
            this.serviceName = serviceName;
            counter = new AtomicInteger(0);
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "New Relic " + serviceName + " #" + counter.incrementAndGet());
            thread.setDaemon(daemon);
            return thread;
        }
    }
}
