/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// this is a simplified copy of the DefaultThreadFactory
public class DefaultThreadFactory implements ThreadFactory {
    /**
     * The name created threads will use.
     */
    private final String name;
    /**
     * Whether the created thread is a daemon thread.
     */
    private final boolean daemon;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    /**
     * Constructs a thread factory that will created named threads.
     */
    public DefaultThreadFactory(String name, boolean daemon) {
        this.name = name;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        int num = threadNumber.getAndIncrement();
        Thread t = new Thread(r, name + " " + num);
        if (daemon) {
            t.setDaemon(true);
        }
        return t;
    }
}
