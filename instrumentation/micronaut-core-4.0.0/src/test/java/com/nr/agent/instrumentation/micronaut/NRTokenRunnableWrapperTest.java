/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut;

import com.newrelic.api.agent.Token;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NRTokenRunnableWrapperTest {

    @Test
    public void run_linksAndExpiresTokenBeforeRunningDelegate() {
        Token token = mock(Token.class);
        Runnable delegate = mock(Runnable.class);

        new NRTokenRunnableWrapper(delegate, token).run();

        InOrder order = inOrder(token, delegate);
        order.verify(token).linkAndExpire();
        order.verify(delegate).run();
    }

    @Test
    public void run_returnsDelegateExecution() {
        Token token = mock(Token.class);
        boolean[] ran = new boolean[1];
        Runnable delegate = () -> ran[0] = true;

        new NRTokenRunnableWrapper(delegate, token).run();

        assertEquals(true, ran[0]);
    }

    @Test
    public void run_doesNotRelinkOnSecondInvocation() {
        Token token = mock(Token.class);
        Runnable delegate = mock(Runnable.class);
        NRTokenRunnableWrapper wrapper = new NRTokenRunnableWrapper(delegate, token);

        wrapper.run();
        wrapper.run();

        verify(token, times(1)).linkAndExpire();
        verify(delegate, times(2)).run();
    }

    @Test
    public void run_toleratesNullToken() {
        Runnable delegate = mock(Runnable.class);

        new NRTokenRunnableWrapper(delegate, null).run();

        verify(delegate).run();
    }
}
