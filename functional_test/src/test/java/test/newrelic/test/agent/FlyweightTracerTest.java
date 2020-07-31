/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpSegment;
import com.newrelic.agent.bridge.NoOpToken;
import com.newrelic.agent.bridge.TracedActivity;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

public class FlyweightTracerTest {

    @Trace(dispatcher = true)
    @Test
    public void test() {
        int count = 100000;
        callManyTimes(count);

        Transaction transaction = Transaction.getTransaction();
        TransactionStats transactionStats = transaction.getTransactionActivity().getTransactionStats();
        ResponseTimeStats time = transactionStats.getScopedStats().getOrCreateResponseTimeStats(
                "Java/test.newrelic.test.agent.FlyweightTracerTest/fastMethodDude");

        ResponseTimeStats dude = transactionStats.getUnscopedStats().getOrCreateResponseTimeStats("Dude");
        ResponseTimeStats man = transactionStats.getUnscopedStats().getOrCreateResponseTimeStats("Man");

        Assert.assertEquals(count, time.getCallCount());
        Assert.assertEquals(count, dude.getCallCount());
        Assert.assertEquals(count, man.getCallCount());
    }

    @Trace(dispatcher = true)
    @Test
    public void testGetTokenFromFlyweight() {
        getTokenFromFlyweight();
    }

    @Trace(dispatcher = true)
    @Test
    public void testTracedActivityFromFlyweight() {
        tracedActivityFromFlyweight();
    }

    @Trace(dispatcher = true)
    private void callManyTimes(int count) {

        for (int i = 0; i < count; i++) {
            fastMethod(true);
        }
    }

    @Trace(excludeFromTransactionTrace = true, leaf = true, rollupMetricName = { "Dude", "Man" })
    private void fastMethod(boolean call) {

        // this has no effect
        AgentBridge.getAgent().getTracedMethod().nameTransaction(TransactionNamePriority.FRAMEWORK_HIGH);

        // and use the public api to access the current method
        NewRelic.getAgent().getTracedMethod().setMetricName(
                NewRelic.getAgent().getTracedMethod().getMetricName() + "Dude");

        // since this is a leaf, child calls should be ignored. Verify that with our counts
        if (call) {
            fastMethod(false);
        }
    }

    @Trace(excludeFromTransactionTrace = true, leaf = true)
    private void getTokenFromFlyweight() {
        // When you are in a flyweight method you should not be allowed to get a token for async linking due to the
        // fact that we have no real parent tracer to tie the token to. We can revisit this in the future if need be
        // but it would require a significant number of changes to the agent
        Token token = AgentBridge.getAgent().getTransaction().getToken();
        Assert.assertNotNull(token);
        Assert.assertFalse(token.isActive());
        Assert.assertTrue(token instanceof NoOpToken);
    }

    @Trace(excludeFromTransactionTrace = true, leaf = true)
    private void tracedActivityFromFlyweight() {
        // When you are in a flyweight method you should not be allowed to start a traced activity due to the
        // fact that we have no real parent tracer to tie the tracer to. We can revisit this in the future if need be
        // but it would require a significant number of changes to the agent
        TracedActivity tracedActivity = AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
        Assert.assertTrue(tracedActivity instanceof NoOpSegment);
    }
}
