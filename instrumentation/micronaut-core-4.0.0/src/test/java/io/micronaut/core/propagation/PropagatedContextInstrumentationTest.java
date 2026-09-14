/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.core.propagation;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.micronaut.NRTokenCallableWrapper;
import com.nr.agent.instrumentation.micronaut.NRTokenRunnableWrapper;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class PropagatedContextInstrumentationTest {

    @Test
    public void wrapRunnable_noTransaction_returnsOriginalUnwrapped() {
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class);
                MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            Runnable original = mock(Runnable.class);
            weaver.when(Weaver::callOriginal).thenReturn(original);

            Agent agent = mock(Agent.class);
            newRelic.when(NewRelic::getAgent).thenReturn(agent);
            when(agent.getTransaction()).thenReturn(null);

            Runnable result = new PropagatedContext_Instrumentation().wrap(original);

            assertSame("with no transaction there is nothing to relink, so the original must pass through untouched",
                    original, result);
        }
    }

    @Test
    public void wrapRunnable_noToken_returnsOriginalUnwrapped() {
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class);
                MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            Runnable original = mock(Runnable.class);
            weaver.when(Weaver::callOriginal).thenReturn(original);

            Agent agent = mock(Agent.class);
            Transaction transaction = mock(Transaction.class);
            newRelic.when(NewRelic::getAgent).thenReturn(agent);
            when(agent.getTransaction()).thenReturn(transaction);
            when(transaction.getToken()).thenReturn(null);

            Runnable result = new PropagatedContext_Instrumentation().wrap(original);

            assertSame(original, result);
        }
    }

    @Test
    public void wrapRunnable_withTransactionAndToken_wrapsWithTokenThatRelinksOnRun() {
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class);
                MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            Runnable original = mock(Runnable.class);
            weaver.when(Weaver::callOriginal).thenReturn(original);

            Agent agent = mock(Agent.class);
            Transaction transaction = mock(Transaction.class);
            Token token = mock(Token.class);
            newRelic.when(NewRelic::getAgent).thenReturn(agent);
            when(agent.getTransaction()).thenReturn(transaction);
            when(transaction.getToken()).thenReturn(token);

            Runnable result = new PropagatedContext_Instrumentation().wrap(original);

            assertTrue("the offloaded-thread hop must be wrapped so the token can be relinked when it runs",
                    result instanceof NRTokenRunnableWrapper);
            result.run();
            org.mockito.Mockito.verify(token).linkAndExpire();
            org.mockito.Mockito.verify(original).run();
        }
    }

    @Test
    public void wrapCallable_withTransactionAndToken_wrapsWithTokenThatRelinksOnCall() throws Exception {
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class);
                MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            @SuppressWarnings("unchecked")
            Callable<String> original = mock(Callable.class);
            when(original.call()).thenReturn("result");
            weaver.when(Weaver::callOriginal).thenReturn(original);

            Agent agent = mock(Agent.class);
            Transaction transaction = mock(Transaction.class);
            Token token = mock(Token.class);
            newRelic.when(NewRelic::getAgent).thenReturn(agent);
            when(agent.getTransaction()).thenReturn(transaction);
            when(transaction.getToken()).thenReturn(token);

            Callable<String> result = new PropagatedContext_Instrumentation().wrap(original);

            assertTrue(result instanceof NRTokenCallableWrapper);
            assertSame("result", result.call());
            org.mockito.Mockito.verify(token).linkAndExpire();
        }
    }

    @Test
    public void wrapCallable_noTransaction_returnsOriginalUnwrapped() {
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class);
                MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            @SuppressWarnings("unchecked")
            Callable<String> original = mock(Callable.class);
            weaver.when(Weaver::callOriginal).thenReturn(original);

            Agent agent = mock(Agent.class);
            newRelic.when(NewRelic::getAgent).thenReturn(agent);
            when(agent.getTransaction()).thenReturn(null);

            Callable<String> result = new PropagatedContext_Instrumentation().wrap(original);

            assertSame(original, result);
        }
    }
}
