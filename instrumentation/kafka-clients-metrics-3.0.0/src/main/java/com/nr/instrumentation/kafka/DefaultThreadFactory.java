package com.nr.instrumentation.kafka;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// This is a simplified copy of the DefaultThreadFactory.
// The solution is based on HttpURLConnection's solution.
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
        String threadName = num == 1 ? name : name + " " + num;
        Thread t = new Thread(r, threadName);
        if (daemon) {
            t.setDaemon(true);
        }
        return t;
    }
}