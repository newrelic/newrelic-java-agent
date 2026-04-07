/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.impl;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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

        Field listenerField = FutureImpl.class.getDeclaredField("handler");
        listenerField.setAccessible(true);

        FutureImpl<Object> future = new FutureImpl<>();
        Handler<AsyncResult<Object>> listener1 = new NoopHandler<>();
        Handler<AsyncResult<Object>> listener2 = new NoopHandler<>();
        Handler<AsyncResult<Object>> listener3 = new NoopHandler<>();

        assertTrue(tokenMap.isEmpty());

        future.onComplete(listener1);
        assertNotNull(tokenMap.get(listener1));

        future.onComplete(listener2);
        assertNull(tokenMap.get(listener1));
        assertNull(tokenMap.get(listener2));
        assertEquals(1, tokenMap.size());
        Token listenerArrayToken = tokenMap.get(listenerField.get(future));
        assertNotNull(listenerArrayToken);

        future.onComplete(listener3);
        assertNull(tokenMap.get(listener3));
        assertEquals(1, tokenMap.size());
        assertSame(listenerArrayToken, tokenMap.get(listenerField.get(future)));
    }

    private static class NoopHandler<T> implements Handler<AsyncResult<T>> {
        @Override
        public void handle(AsyncResult<T> t) {

        }
    }
}
