/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ThreadService.AgentThread;
import com.newrelic.agent.service.ServiceFactory;

public class DefaultThreadFactory implements ThreadFactory {
    /** The name created threads will use. */
    private final String name;
    /** Whether or not the created thread is a daemon thread. */
    private final boolean daemon;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    /** Constructs a thread factory that will created named threads. */
    public DefaultThreadFactory(String name, boolean daemon) {
        this.name = name;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        int num = threadNumber.getAndIncrement();
        String threadName = num == 1 ? name : name + " " + num;
        Thread t = new AgentThreadImpl(r, threadName);
        Agent.LOG.fine("Created agent thread: " + t.getName());
        ServiceFactory.getThreadService().registerAgentThreadId(t.getId());
        if (daemon) {
            t.setDaemon(true);
        }
        return t;
    }

    private static class AgentThreadImpl extends Thread implements AgentThread {

        public AgentThreadImpl(Runnable r, String threadName) {
            super(r, threadName);
        }

    }
}
