/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;

public class TracedMethodTest {

    @Test
    public void testAllTracedMethods() {
        com.newrelic.api.agent.TracedMethod outsideFromAgent = NewRelic.getAgent().getTracedMethod();
        com.newrelic.api.agent.TracedMethod outsideFromTx = NewRelic.getAgent().getTransaction().getTracedMethod();
        Assert.assertEquals(outsideFromTx, NoOpTracedMethod.INSTANCE);
        Assert.assertEquals(outsideFromAgent, NoOpTracedMethod.INSTANCE);

        com.newrelic.api.agent.TracedMethod tm = myTest();

        outsideFromAgent = NewRelic.getAgent().getTracedMethod();
        outsideFromTx = NewRelic.getAgent().getTransaction().getTracedMethod();
        Assert.assertEquals(outsideFromTx, NoOpTracedMethod.INSTANCE);
        Assert.assertEquals(outsideFromAgent, NoOpTracedMethod.INSTANCE);
        Assert.assertNotEquals(tm, outsideFromAgent);

    }

    @Trace(dispatcher = true)
    public com.newrelic.api.agent.TracedMethod myTest() {
        com.newrelic.api.agent.TracedMethod tmFromAgent = NewRelic.getAgent().getTracedMethod();
        com.newrelic.api.agent.TracedMethod tmFromTx = NewRelic.getAgent().getTransaction().getTracedMethod();
        Assert.assertEquals(tmFromAgent, tmFromTx);
        com.newrelic.api.agent.TracedMethod inMethod = myOther();
        Assert.assertNotEquals(tmFromAgent, inMethod);
        return tmFromAgent;
    }

    @Trace
    public com.newrelic.api.agent.TracedMethod myOther() {
        com.newrelic.api.agent.TracedMethod tmFromAgent = NewRelic.getAgent().getTracedMethod();
        Assert.assertEquals(tmFromAgent, NewRelic.getAgent().getTransaction().getTracedMethod());
        return tmFromAgent;
    }

    @Test
    public void testOneTracedMethod() {
        com.newrelic.api.agent.TracedMethod outsideFromAgent = NewRelic.getAgent().getTracedMethod();
        com.newrelic.api.agent.TracedMethod outsideFromTx = NewRelic.getAgent().getTransaction().getTracedMethod();
        Assert.assertEquals(outsideFromTx, NoOpTracedMethod.INSTANCE);
        Assert.assertEquals(outsideFromAgent, NoOpTracedMethod.INSTANCE);

        com.newrelic.api.agent.TracedMethod tm = myTestOneTracer();

        outsideFromAgent = NewRelic.getAgent().getTracedMethod();
        outsideFromTx = NewRelic.getAgent().getTransaction().getTracedMethod();
        Assert.assertNotEquals(tm, outsideFromAgent);
        Assert.assertNotEquals(tm, outsideFromTx);
        Assert.assertEquals(outsideFromTx, NoOpTracedMethod.INSTANCE);
        Assert.assertEquals(outsideFromAgent, NoOpTracedMethod.INSTANCE);

    }

    @Test
    public void noTransaction() {
        com.newrelic.agent.Transaction.clearTransaction();

        NewRelic.getAgent().getTracedMethod();

        Assert.assertNull(AgentBridge.getAgent().getTransaction(false));
    }

    @Trace(dispatcher = true)
    public com.newrelic.api.agent.TracedMethod myTestOneTracer() {
        com.newrelic.api.agent.TracedMethod tmFromAgent = NewRelic.getAgent().getTracedMethod();
        com.newrelic.api.agent.TracedMethod tmFromTx = NewRelic.getAgent().getTransaction().getTracedMethod();
        Assert.assertEquals(tmFromAgent, tmFromTx);
        com.newrelic.api.agent.TracedMethod inMethod = myOtherNoTrace();
        Assert.assertEquals(tmFromAgent, inMethod);
        return tmFromAgent;
    }

    public com.newrelic.api.agent.TracedMethod myOtherNoTrace() {
        com.newrelic.api.agent.TracedMethod tmFromAgent = NewRelic.getAgent().getTracedMethod();
        Assert.assertEquals(tmFromAgent, NewRelic.getAgent().getTransaction().getTracedMethod());
        return tmFromAgent;
    }

}
