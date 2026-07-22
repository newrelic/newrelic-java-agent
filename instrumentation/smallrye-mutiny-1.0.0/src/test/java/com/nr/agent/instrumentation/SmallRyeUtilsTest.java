/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"io.smallrye.mutiny"})
public class SmallRyeUtilsTest {
    @Test
    public void getToken_withNoTokenStored_returnsNull() {
        assertNull(SmallRyeUtils.getToken(new Object()));
    }

    @Test
    public void getToken_withStoredToken_returnsToken() {
        Object key = new Object();
        Token token = NewRelic.getAgent().getTransaction().getToken();
        SmallRyeUtils.putToken(key, token);
        assertSame(token, SmallRyeUtils.getToken(key));
        SmallRyeUtils.removeToken(key);
    }

    @Test
    public void putToken_withValidKeyAndToken_storesToken() {
        Object key = new Object();
        Token token = NewRelic.getAgent().getTransaction().getToken();
        SmallRyeUtils.putToken(key, token);
        assertSame(token, SmallRyeUtils.getToken(key));
        SmallRyeUtils.removeToken(key);
    }

    @Test
    public void putToken_calledTwiceWithSameKey_replacesToken() {
        Object key = new Object();
        Token first = NewRelic.getAgent().getTransaction().getToken();
        Token second = NewRelic.getAgent().getTransaction().getToken();
        SmallRyeUtils.putToken(key, first);
        SmallRyeUtils.putToken(key, second);
        assertSame(second, SmallRyeUtils.getToken(key));
        SmallRyeUtils.removeToken(key);
    }

    @Test
    public void removeToken_withStoredToken_removesToken() {
        Object key = new Object();
        SmallRyeUtils.putToken(key, NewRelic.getAgent().getTransaction().getToken());
        SmallRyeUtils.removeToken(key);
        assertNull(SmallRyeUtils.getToken(key));
    }

    @Test
    public void removeToken_withNoStoredToken_doesNotThrow() {
        SmallRyeUtils.removeToken(new Object());
    }

    @Test
    public void assignTokenToSubscriber_withNullSubscriber_doesNotThrow() {
        SmallRyeUtils.assignTokenToSubscriber(null);
    }

    @Test
    public void assignTokenToSubscriber_withNoActiveTransaction_doesNotStoreToken() {
        Object subscriber = new Object();
        SmallRyeUtils.assignTokenToSubscriber(subscriber);
        assertNull(SmallRyeUtils.getToken(subscriber));
    }

    @Test
    public void assignTokenToSubscriber_withActiveTransaction_storesToken() {
        dispatcherForTest();
    }

    @Trace(dispatcher = true)
    private void dispatcherForTest() {
        Object subscriber = new Object();
        SmallRyeUtils.assignTokenToSubscriber(subscriber);
        assertNotNull(SmallRyeUtils.getToken(subscriber));
        SmallRyeUtils.removeToken(subscriber);
    }

    @Test
    public void assignTokenToSubscriber_withExistingToken_doesNotReplaceExistingToken() {
        dispatcherForTest2();
    }

    @Trace(dispatcher = true)
    private void dispatcherForTest2() {
        Object subscriber = new Object();
        Token existing = NewRelic.getAgent().getTransaction().getToken();
        SmallRyeUtils.putToken(subscriber, existing);
        SmallRyeUtils.assignTokenToSubscriber(subscriber);
        assertSame(existing, SmallRyeUtils.getToken(subscriber));
        SmallRyeUtils.removeToken(subscriber);
    }

    @Test
    public void assignTokenToSubscriber_calledTwice_doesNotReplaceTokenOnSecondCall() {
        dispatcherForTest3();
    }

    @Trace(dispatcher = true)
    private void dispatcherForTest3() {
        Object subscriber = new Object();
        SmallRyeUtils.assignTokenToSubscriber(subscriber);
        Token first = SmallRyeUtils.getToken(subscriber);
        assertNotNull(first);
        SmallRyeUtils.assignTokenToSubscriber(subscriber);
        assertSame(first, SmallRyeUtils.getToken(subscriber));
        SmallRyeUtils.removeToken(subscriber);
    }
}
