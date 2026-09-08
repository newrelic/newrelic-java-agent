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

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NRTokenCallableWrapperTest {

    @Test
    public void call_linksAndExpiresTokenBeforeCallingDelegate() throws Exception {
        Token token = mock(Token.class);
        @SuppressWarnings("unchecked")
        Callable<String> delegate = mock(Callable.class);

        new NRTokenCallableWrapper<>(delegate, token).call();

        InOrder order = inOrder(token, delegate);
        order.verify(token).linkAndExpire();
        order.verify(delegate).call();
    }

    @Test
    public void call_returnsDelegateResult() throws Exception {
        Token token = mock(Token.class);
        Callable<String> delegate = () -> "result";

        String result = new NRTokenCallableWrapper<>(delegate, token).call();

        assertEquals("result", result);
    }

    @Test
    public void call_doesNotRelinkOnSecondInvocation() throws Exception {
        Token token = mock(Token.class);
        @SuppressWarnings("unchecked")
        Callable<String> delegate = mock(Callable.class);
        NRTokenCallableWrapper<String> wrapper = new NRTokenCallableWrapper<>(delegate, token);

        wrapper.call();
        wrapper.call();

        verify(token, times(1)).linkAndExpire();
        verify(delegate, times(2)).call();
    }

    @Test
    public void call_toleratesNullToken() throws Exception {
        Callable<String> delegate = () -> "result";

        String result = new NRTokenCallableWrapper<>(delegate, null).call();

        assertEquals("result", result);
    }
}
