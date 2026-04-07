/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl.future;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.impl.future.FutureImpl;
import io.vertx.core.impl.future.Listener;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class FutureTokenTest {

    @Test
    public void futureCreatesAtMost2Tokens() throws NoSuchFieldException, IllegalAccessException {
        tokensTest();
    }


    @Trace(dispatcher = true)
    private void tokensTest() throws NoSuchFieldException, IllegalAccessException {
        Field tokenField = VertxCoreUtil.class.getDeclaredField("tokenMap");
        tokenField.setAccessible(true);
        Map<Object, Token> tokenMap = (Map<Object, Token>) tokenField.get(null);

        Field listenerField = FutureImpl.class.getDeclaredField("listener");
        listenerField.setAccessible(true);

        FutureImpl<Object> future = new FutureImpl<>();
        Listener<Object> listener1 = new NoopListener<>();
        Listener<Object> listener2 = new NoopListener<>();
        Listener<Object> listener3 = new NoopListener<>();

        assertTrue(tokenMap.isEmpty());

        future.addListener(listener1);
        assertNotNull(tokenMap.get(listener1));

        future.addListener(listener2);
        assertNull(tokenMap.get(listener1));
        assertNull(tokenMap.get(listener2));
        assertEquals(1, tokenMap.size());
        Token listenerArrayToken = tokenMap.get(listenerField.get(future));
        assertNotNull(listenerArrayToken);

        future.addListener(listener3);
        assertNull(tokenMap.get(listener3));
        assertEquals(1, tokenMap.size());
        assertSame(listenerArrayToken, tokenMap.get(listenerField.get(future)));
    }

    private static class NoopListener<T> implements Listener<T> {
        @Override
        public void onSuccess(T o) {

        }

        @Override
        public void onFailure(Throwable throwable) {

        }
    }

}
