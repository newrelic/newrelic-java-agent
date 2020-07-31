/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ExcludeRootAsyncTracersWithTokens {

    /**
     * Make sure @Trace(async = true, excludeFromTransactionTrace = true) works as expected with the Token API.
     *
     * The tracer should only be excluded if it is not a root tracer. Additionally, regular tracers created under the
     * excluded tracer should still appear in the TT.
     */
    @Test
    public void testRegisterStartSameThread() {
        Transaction tx = new SingleThreadWorker().run();
        List<Tracer> tracers = tx.getTracers();
        Assert.assertEquals(2, tracers.size());

        // assert: transactionWork -> moreWork() -> [innerWork, regularWork()];
        Map<Tracer, Collection<Tracer>> children = TransactionTrace.buildChildren(tracers);

        Collection<Tracer> ttChildrenMap = children.get(tx.getRootTracer());
        Assert.assertNotNull(ttChildrenMap);
        Assert.assertEquals(2, ttChildrenMap.size()); // the root moreWork()

        Iterator<Tracer> iter = ttChildrenMap.iterator();
        Assert.assertNotNull(iter);

        Tracer trace1 = iter.next();
        Assert.assertEquals("Custom/com.newrelic.agent.async.ExcludeRootAsyncTracersWithTokens$SingleThreadWorker/regularWork", trace1.getMetricName());
        ttChildrenMap = children.get(trace1);
        Assert.assertNull(ttChildrenMap);

        Tracer trace2 = iter.next();
        Assert.assertEquals("Java/com.newrelic.agent.async.ExcludeRootAsyncTracersWithTokens$SingleThreadWorker/innerWork", trace2.getMetricName());
        ttChildrenMap = children.get(trace2);
        Assert.assertNull(ttChildrenMap);
    }

    private static class SingleThreadWorker {

        public Transaction run() {
            return transactionWork();
        }

        @Trace(dispatcher = true)
        public Transaction transactionWork() {
            AgentBridge.getAgent().getLogger().log(Level.INFO, "starting transaction");
            Transaction txn = Transaction.getTransaction(false);
            moreWork(true);
            return txn;
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void moreWork(boolean asyncWork) {
            if (asyncWork) {
                final Token token = AgentBridge.getAgent().getTransaction(false).getToken();
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        innerWork(token);
                    }
                };

                thread.start();
                try {
                    thread.join();
                } catch (Exception e) {
                }

                moreWork(false);
            } else {
                regularWork();
            }
        }

        @Trace(async = true)
        public void innerWork(Token token) {
            token.linkAndExpire();
            AgentBridge.getAgent().getLogger().log(Level.INFO, "in innerWork");
        }

        @Trace
        public void regularWork() {
            AgentBridge.getAgent().getLogger().log(Level.INFO, "in regularWork");
        }
    }

}
