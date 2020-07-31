/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;

public class ErrorsInAsyncApiTest extends AsyncTest {

    @Test(timeout = 120000)
    public void testExceptionInParent() throws Exception {
        final Thread child = new Thread() {
            private String threadName;

            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().startAsyncActivity(this);
                threadName = Thread.currentThread().getName();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }

            public String getThreadName() {
                return threadName;
            }
        };

        final Thread parent = new Thread() {
            private String threadName;

            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                threadName = Thread.currentThread().getName();
                child.start();
                throw new RuntimeException("From Parent");
            }

            public String getThreadName() {
                return threadName;
            }
        };

        parent.start();
        parent.join();
        child.join();

        verifyScopedMetricsPresent(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/", parent,
                "/run"), fmtMetric("Java/", child, "/run"));
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime", fmtMetric(
                "OtherTransaction/Custom/", parent, "/run"), fmtMetric("OtherTransactionTotalTime/Custom/", parent,
                "/run"));
        verifyTransactionSegmentsBreadthFirst(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/",
                parent, "/run"), parent.getName(), fmtMetric("Java/", child, "/run"), child.getName());

        Assert.assertNotNull(data.getThrowable());
        Assert.assertEquals("From Parent", data.getThrowable().throwable.getMessage());
    }

    @Test(timeout = 120000)
    public void testExceptionInChild() throws Exception {
        final Thread child = new Thread() {

            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().startAsyncActivity(this);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
                throw new RuntimeException("From Child");
            }
        };

        final Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                child.start();
            }
        };

        parent.start();
        parent.join();
        child.join();

        verifyScopedMetricsPresent(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/", parent,
                "/run"), fmtMetric("Java/", child, "/run"));
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime", fmtMetric(
                "OtherTransaction/Custom/", parent, "/run"), fmtMetric("OtherTransactionTotalTime/Custom/", parent,
                "/run"));
        verifyTransactionSegmentsBreadthFirst(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/",
                parent, "/run"), parent.getName(), fmtMetric("Java/", child, "/run"), child.getName());

        Assert.assertNull(data.getThrowable());
    }

    @Test(timeout = 120000)
    public void testExceptionInBoth() throws Exception {
        final Thread child = new Thread() {

            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().startAsyncActivity(this);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
                throw new RuntimeException("From Child");
            }
        };

        final Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                child.start();
                throw new RuntimeException("From Parent");
            }
        };

        parent.start();
        parent.join();
        child.join();

        verifyScopedMetricsPresent(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/", parent,
                "/run"), fmtMetric("Java/", child, "/run"));
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime", fmtMetric(
                "OtherTransaction/Custom/", parent, "/run"), fmtMetric("OtherTransactionTotalTime/Custom/", parent,
                "/run"));
        verifyTransactionSegmentsBreadthFirst(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/",
                parent, "/run"), parent.getName(), fmtMetric("Java/", child, "/run"), child.getName());

        Assert.assertNotNull(data.getThrowable());
        Assert.assertEquals("From Parent", data.getThrowable().throwable.getMessage());
    }
}
