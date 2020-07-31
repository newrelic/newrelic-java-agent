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

public class EdgeCasesInAsyncApiTest extends AsyncTest {

    @Test(timeout = 120000)
    public void testFalseAsync() throws Exception {

        final Runnable child = new Runnable() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().startAsyncActivity(this);
            }
        };

        final Runnable parent = new Runnable() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                child.run();
            }
        };

        final Thread thread = new Thread() {
            @Override
            public void run() {
                // Run parent, which runs child in the same thread.
                parent.run();
            }
        };

        thread.start();
        thread.join();

        verifyScopedMetricsPresent(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/", parent,
                "/run"), fmtMetric("Java/", child, "/run"));
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime", fmtMetric(
                "OtherTransaction/Custom/", parent, "/run"), fmtMetric("OtherTransactionTotalTime/Custom/", parent,
                "/run"));
        verifyTransactionSegmentsBreadthFirst(fmtMetric("OtherTransaction/Custom/", parent, "/run"), fmtMetric("Java/",
                parent, "/run"), thread.getName(), fmtMetric("Java/", child, "/run"), NO_ASYNC_CONTEXT);

        Assert.assertNull(data.getThrowable());
    }

    @Test(timeout = 120000)
    public void testReuseContext() throws Exception {
        final Object context = new Object();

        final Thread child1 = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().startAsyncActivity(context);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        };

        final Thread child2 = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().startAsyncActivity(context);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        };

        final Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(context);
                // Second call should do nothing:
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(context);
                child1.start();
                try {
                    child1.join();
                } catch (InterruptedException e) {
                }
                child2.start();
                try {
                    child2.join();
                } catch (InterruptedException e) {
                }
            }
        };

        parent.start();
        parent.join();

        verifyScopedMetricsPresent(statsList.get(0), dataList.get(0), fmtMetric("OtherTransaction/Custom/", child2,
                "/run"), fmtMetric("Java/", child2, "/run"));
        verifyScopedMetricsPresent(statsList.get(1), dataList.get(1), fmtMetric("OtherTransaction/Custom/", parent,
                "/run"), fmtMetric("Java/", parent, "/run"), fmtMetric("Java/", child1, "/run"));

        verifyUnscopedMetricsPresent(statsList.get(0), 1, "OtherTransaction/all", "OtherTransactionTotalTime", fmtMetric(
                "OtherTransaction/Custom/", child2, "/run"), fmtMetric("OtherTransactionTotalTime/Custom/", child2,
                "/run"));
        verifyUnscopedMetricsPresent(statsList.get(1), 1, "OtherTransaction/all", "OtherTransactionTotalTime", fmtMetric("OtherTransaction/Custom/", parent, "/run"),
                fmtMetric("OtherTransactionTotalTime/Custom/", parent, "/run"));

        verifyTransactionSegmentsBreadthFirst(dataList.get(0), fmtMetric("OtherTransaction/Custom/", child2, "/run"),
                fmtMetric("Java/", child2, "/run"), child2.getName());
        verifyTransactionSegmentsBreadthFirst(dataList.get(1), fmtMetric("OtherTransaction/Custom/", parent, "/run"),
                fmtMetric("Java/", parent, "/run"), parent.getName(), fmtMetric("Java/", child1, "/run"),
                child1.getName());

        Assert.assertNull(data.getThrowable());
    }
}
