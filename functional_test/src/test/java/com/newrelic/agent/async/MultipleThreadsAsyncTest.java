/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MultipleThreadsAsyncTest extends AsyncTest {
    private static final List<StartAnotherThreadWaitForChild> threads = new ArrayList<>();
    final List<ChildThreadCalculateCpu> cpuThreads = new ArrayList<>();

    @Test(timeout = 30000)
    public void testCpuDepth() throws InterruptedException {
        final long[] parentCpu = new long[1];
        Thread parent = new Thread() {

            @Trace(dispatcher = true)
            @Override
            public void run() {
                long startTime = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime();
                ChildThreadCalculateCpu initial = new ChildThreadCalculateCpu(4);
                cpuThreads.add(initial);
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(initial);
                initial.start();
                try {
                    initial.join();
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                long endTime = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime();
                parentCpu[0] = (endTime - startTime);
            }
        };
        parent.start();
        parent.join();

        verifyScopedMetricsPresent(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/", parent,
                "/run"));
        Assert.assertEquals(5, stats.getScopedStats().getOrCreateResponseTimeStats(
                fmtMetric("Java/", ChildThreadCalculateCpu.class, "/run")).getCallCount());
        verifyTransactionSegmentsBreadthFirst(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/",
                parent, "/run"), parent.getName(), fmtMetric("Java/", cpuThreads.get(0), "/run"),
                cpuThreads.get(0).getName(), fmtMetric("Java/", cpuThreads.get(1), "/run"),
                cpuThreads.get(1).getName(), fmtMetric("Java/", cpuThreads.get(2), "/run"),
                cpuThreads.get(2).getName(), fmtMetric("Java/", cpuThreads.get(3), "/run"),
                cpuThreads.get(3).getName(), fmtMetric("Java/", cpuThreads.get(4), "/run"), cpuThreads.get(4).getName());
        verifyNoExceptions();

        verifyCpu(calculateMinTotalCpu(parentCpu[0]));
    }

    // start five threads, each thread waits for child to finish before exiting
    @Test(timeout = 30000)
    public void testcpuBreadth() throws InterruptedException {
        final StartAnotherThreadCpu e = new StartAnotherThreadCpu(null);
        final StartAnotherThreadCpu d = new StartAnotherThreadCpu(e);
        final StartAnotherThreadCpu c = new StartAnotherThreadCpu(d);
        final StartAnotherThreadCpu b = new StartAnotherThreadCpu(c);
        final StartAnotherThreadCpu a = new StartAnotherThreadCpu(b);

        Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(a);
                a.start();
                try {
                    a.join();
                    b.join();
                    c.join();
                    d.join();
                    e.join();
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                }
            }
        };
        parent.start();
        parent.join();

        verifyScopedMetricsPresent(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/", parent,
                "/run"));
        Assert.assertEquals(5, stats.getScopedStats().getOrCreateResponseTimeStats(
                fmtMetric("Java/", StartAnotherThreadCpu.class, "/run")).getCallCount());
        verifyTransactionSegmentsBreadthFirst(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/",
                parent, "/run"), parent.getName(), fmtMetric("Java/", StartAnotherThreadCpu.class, "/run"),
                a.getName(), fmtMetric("Java/", StartAnotherThreadCpu.class, "/run"), b.getName(), fmtMetric("Java/",
                        StartAnotherThreadCpu.class, "/run"), c.getName(), fmtMetric("Java/",
                        StartAnotherThreadCpu.class, "/run"), d.getName(), fmtMetric("Java/",
                        StartAnotherThreadCpu.class, "/run"), e.getName());
        verifyNoExceptions();
        verifyCpu(e.getCpuTime() + d.getCpuTime() + c.getCpuTime() + b.getCpuTime() + a.getCpuTime());
    }

    class StartAnotherThreadCpu extends Thread {

        private final Thread thread;
        private long cpuTimeForThread;

        public StartAnotherThreadCpu(Thread anotherThread) {
            thread = anotherThread;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            long startTime = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime();
            try {
                AgentBridge.getAgent().startAsyncActivity(this);

                // burn cpu
                for (int i = 0; i < 50; i++) {
                    String obfuscated = Obfuscator.obfuscateNameUsingKey("test", "1#23");
                    String deobfuscated = Obfuscator.deobfuscateNameUsingKey(obfuscated, "1#23");
                }

                if (thread != null) {
                    AgentBridge.getAgent().getTransaction().registerAsyncActivity(thread);
                    thread.start();
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                // ignore
            }
            long endTime = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime();
            cpuTimeForThread = endTime - startTime;
        }

        public long getCpuTime() {
            return cpuTimeForThread;
        }
    }

    private long calculateMinTotalCpu(long init) {
        long output = init;
        for (ChildThreadCalculateCpu current : cpuThreads) {
            output += current.getCpuTime();
        }
        return output;
    }

    class ChildThreadCalculateCpu extends Thread {

        private final int value;
        private long cpuTimeForThread;

        public ChildThreadCalculateCpu(int count) {
            value = count;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            long startTime = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime();
            try {
                AgentBridge.getAgent().startAsyncActivity(this);

                // burn cpu
                for (int i = 0; i < 50; i++) {
                    String obfuscated = Obfuscator.obfuscateNameUsingKey("test", "1#23");
                    String deobfuscated = Obfuscator.deobfuscateNameUsingKey(obfuscated, "1#23");
                }

                // start child
                if (value > 0) {
                    ChildThreadCalculateCpu nextThread = new ChildThreadCalculateCpu(value - 1);
                    cpuThreads.add(nextThread);
                    AgentBridge.getAgent().getTransaction().registerAsyncActivity(nextThread);

                    nextThread.start();
                    nextThread.join();
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                // ignore
            }
            long endTime = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime();
            cpuTimeForThread = endTime - startTime;
        }

        public long getCpuTime() {
            return cpuTimeForThread;
        }
    }

    // start five threads, each thread waits for child to finish before exiting
    @Test(timeout = 30000)
    public void testFiveThreadsWaitForChild() throws InterruptedException {
        Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                StartAnotherThreadWaitForChild initial = new StartAnotherThreadWaitForChild(4);
                threads.add(initial);
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(initial);
                initial.start();
                try {
                    initial.join();
                } catch (InterruptedException e) {
                }
            }
        };
        parent.start();
        parent.join();

        verifyScopedMetricsPresent(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/", parent,
                "/run"));
        Assert.assertEquals(5, stats.getScopedStats().getOrCreateResponseTimeStats(
                fmtMetric("Java/", StartAnotherThreadWaitForChild.class, "/run")).getCallCount());
        verifyTransactionSegmentsBreadthFirst(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/",
                parent, "/run"), parent.getName(), fmtMetric("Java/", threads.get(0), "/run"),
                threads.get(0).getName(), fmtMetric("Java/", threads.get(1), "/run"), threads.get(1).getName(),
                fmtMetric("Java/", threads.get(2), "/run"), threads.get(2).getName(), fmtMetric("Java/",
                        threads.get(3), "/run"), threads.get(3).getName(), fmtMetric("Java/", threads.get(4), "/run"),
                threads.get(4).getName());
        verifyNoExceptions();
    }

    class StartAnotherThreadWaitForChild extends Thread {

        private final int value;

        public StartAnotherThreadWaitForChild(int count) {
            value = count;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            try {
                AgentBridge.getAgent().startAsyncActivity(this);
                if (value > 0) {
                    StartAnotherThreadWaitForChild nextThread = new StartAnotherThreadWaitForChild(value - 1);
                    threads.add(nextThread);
                    AgentBridge.getAgent().getTransaction().registerAsyncActivity(nextThread);
                    nextThread.start();
                    nextThread.join();
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                // ignore
            }

        }
    }

    // start five threads, each thread waits for child to finish before exiting
    @Test(timeout = 30000)
    public void testFiveThreadsFinishImmediately() throws InterruptedException {
        final StartAnotherThreadFinishImmediately e = new StartAnotherThreadFinishImmediately(null);
        final StartAnotherThreadFinishImmediately d = new StartAnotherThreadFinishImmediately(e);
        final StartAnotherThreadFinishImmediately c = new StartAnotherThreadFinishImmediately(d);
        final StartAnotherThreadFinishImmediately b = new StartAnotherThreadFinishImmediately(c);
        final StartAnotherThreadFinishImmediately a = new StartAnotherThreadFinishImmediately(b);

        Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(a);
                a.start();
                try {
                    a.join();
                    b.join();
                    c.join();
                    d.join();
                    e.join();
                } catch (InterruptedException ex) {
                }
            }
        };
        parent.start();
        parent.join();

        verifyScopedMetricsPresent(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/", parent,
                "/run"));
        Assert.assertEquals(5, stats.getScopedStats().getOrCreateResponseTimeStats(
                fmtMetric("Java/", StartAnotherThreadFinishImmediately.class, "/run")).getCallCount());
        verifyTransactionSegmentsBreadthFirst(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/",
                parent, "/run"), parent.getName(),
                fmtMetric("Java/", StartAnotherThreadFinishImmediately.class, "/run"), a.getName(), fmtMetric("Java/",
                        StartAnotherThreadFinishImmediately.class, "/run"), b.getName(), fmtMetric("Java/",
                        StartAnotherThreadFinishImmediately.class, "/run"), c.getName(), fmtMetric("Java/",
                        StartAnotherThreadFinishImmediately.class, "/run"), d.getName(), fmtMetric("Java/",
                        StartAnotherThreadFinishImmediately.class, "/run"), e.getName());
        verifyNoExceptions();
    }

    class StartAnotherThreadFinishImmediately extends Thread {

        private final Thread thread;

        public StartAnotherThreadFinishImmediately(Thread anotherThread) {
            thread = anotherThread;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            try {
                AgentBridge.getAgent().startAsyncActivity(this);
                if (thread != null) {

                    AgentBridge.getAgent().getTransaction().registerAsyncActivity(thread);
                    thread.start();
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
