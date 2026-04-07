/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This implementation of {@link CollectionFactory} is not really meant to be used.
 * It is only implemented in case the agent-bridge is used without the agent, which is an unsupported use case.
 *
 * Therefore, the implementation does not keep the contract from the interface, but returns functional objects
 * so the application will still run.
 */
public class DefaultCollectionFactoryTest {

    @Test
    public void createConcurrentWeakKeyedMap() {
        Map<Object, Object> concurrentWeakKeyedMap = new DefaultCollectionFactory().createConcurrentWeakKeyedMap();
        assertThat(concurrentWeakKeyedMap, instanceOf(Map.class));
    }

    @Test
    public void createConcurrentTimeBasedEvictionMap() {
        Map<Object, Object> concurrentTimeBasedEvictionMap = new DefaultCollectionFactory().createConcurrentTimeBasedEvictionMap(1L);
        assertThat(concurrentTimeBasedEvictionMap, instanceOf(Map.class));
    }

    @Test
    public void memorize() {
        Function<Object, Object> f = mock(Function.class);
        when(f.apply("1")).thenReturn("1");
        when(f.apply("2")).thenReturn("2");
        Function<Object, Object> cache = new DefaultCollectionFactory().memorize(f, 1);
        cache.apply("1");
        cache.apply("1");
        cache.apply("2");
        cache.apply("2");

        // the first call should have been cached, so the function should only have been called once
        Mockito.verify(f, Mockito.times(1)).apply("1");
        // max cache size is 1, so second call should not be cached
        Mockito.verify(f, Mockito.times(2)).apply("2");
    }

    @Test
    public void createAccessTimeBasedCache() {
        Function<Object, Object> accessTimeBasedCache = new DefaultCollectionFactory().createAccessTimeBasedCache(1L, 1, k -> k);
        assertThat(accessTimeBasedCache, instanceOf(Function.class));
    }
}