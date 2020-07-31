package com.newrelic;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {
    private final String serviceName;
    private final AtomicInteger counter = new AtomicInteger(0);

    public DaemonThreadFactory(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName("New Relic " + serviceName + " #" + counter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }
}
