/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ExcludeRootAsyncTracers {

    /**
     * Make sure @Trace(async = true, excludeFromTransactionTrace = true) works as expected with the old async API.
     *
     * The tracer should only be excluded if it is not a root tracer. Additionally, regular tracers created under the
     * excluded tracer should still appear in the TT.
     */
    @Test
    public void testRegisterStartSameThread() throws Exception {
        Transaction tx = new SingleThreadWorker().run();
        List<Tracer> tracers = tx.getTracers();
        Assert.assertEquals(2, tracers.size());

        // assert: transactionWork -> moreWork() -> regularWork();
        Map<Tracer, Collection<Tracer>> children = TransactionTrace.buildChildren(tracers);

        Collection<Tracer> ttChildrenMap = children.get(tx.getRootTracer());
        Assert.assertNotNull(ttChildrenMap);
        Assert.assertEquals(1, ttChildrenMap.size()); // the root moreWork()
        Tracer trace = ttChildrenMap.iterator().next();
        Assert.assertEquals("Java/com.newrelic.agent.async.ExcludeRootAsyncTracers$SingleThreadWorker/moreWork",
                trace.getMetricName());

        ttChildrenMap = children.get(trace);
        Assert.assertNotNull(ttChildrenMap);
        Assert.assertEquals(1, ttChildrenMap.size()); // regularWork() segment
        trace = ttChildrenMap.iterator().next();
        Assert.assertEquals("Custom/com.newrelic.agent.async.ExcludeRootAsyncTracers$SingleThreadWorker/regularWork",
                trace.getMetricName());

        ttChildrenMap = children.get(trace); // no more tracers
        Assert.assertNull(ttChildrenMap);
    }

    private static class SingleThreadWorker {
        public Transaction run() throws Exception {
            final Object context = new Object();
            Transaction tx = transactionWork(context);
            // tx from transactionWork is waiting for a startAsyncActivity
            Thread t = new Thread() {
                @Override
                public void run() {
                    moreWork(context, 0);
                }
            };
            t.start();
            t.join();
            Assert.assertNotNull(tx);
            return tx;
        }

        @Trace(dispatcher = true)
        public Transaction transactionWork(final Object context) {
            AgentBridge.getAgent().getTransaction().registerAsyncActivity(context);
            Transaction tx = Transaction.getTransaction(false);
            return tx;
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void moreWork(final Object context, int i) {
            if (!AgentBridge.getAgent().startAsyncActivity(context)) {
                Assert.fail();
            }
            if (i == 1) {
                regularWork();
            } else {
                final Object context2 = new Object();
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(context2);
                moreWork(context2, ++i);
            }
        }

        @Trace
        public void regularWork() {
            AgentBridge.getAgent().getLogger().log(Level.INFO, "in regularWork");
        }
    }

    private void logChildren(Map<Tracer, Collection<Tracer>> children, Tracer root, int offset) {
        String tabs = "";
        for (int i = 0; i < offset; ++i) {
            tabs += "\t";
        }
        if (null == root) {
            Agent.LOG.log(Level.FINE, "{0} tracers go out:", children.size());
            for (Tracer tracer : children.keySet()) {
                Agent.LOG.log(Level.FINE, tabs + "{0}-{1}", tracer, tracer.getMetricName());
                System.out.println(tabs + tracer + "-" + tracer.getMetricName());
                logChildren(children, tracer, ++offset);
            }
        } else {
            if (null != children.get(root) && children.get(root).size() > 0) {
                for (Tracer tracer : children.get(root)) {
                    Agent.LOG.log(Level.FINE, tabs + "{0}-{1}", tracer, tracer.getMetricName());
                    System.out.println(tabs + tracer + "-" + tracer.getMetricName());
                    logChildren(children, tracer, ++offset);
                }
            }
        }
    }
}
