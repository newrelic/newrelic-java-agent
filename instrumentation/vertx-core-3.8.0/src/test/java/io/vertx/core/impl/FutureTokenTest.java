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
        Handler<AsyncResult<Object>> listener1 = new NoopListener<>();
        Handler<AsyncResult<Object>> listener2 = new NoopListener<>();
        Handler<AsyncResult<Object>> listener3 = new NoopListener<>();

        assertTrue(tokenMap.isEmpty());

        future.setHandler(listener1);
        assertNotNull(tokenMap.get(listener1));
        assertEquals(1, tokenMap.size());

        future.setHandler(listener2);
        assertEquals(1, tokenMap.size());

        future.setHandler(listener3);
        assertEquals(1, tokenMap.size());
        // tests for newer versions of this library have more assertions
        // those assertions are not meaningful here because different supported
        // versions of this library work slightly different
    }

    private static class NoopListener<T> implements Handler<AsyncResult<T>> {
        @Override
        public void handle(AsyncResult<T> tAsyncResult) {

        }
    }

}
