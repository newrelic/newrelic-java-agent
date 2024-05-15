/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.bridge.CollectionFactory;

public class AgentCollectionFactory implements CollectionFactory {

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        Cache<K, V> cache = Caffeine.newBuilder().initialCapacity(32).weakKeys().executor(Runnable::run).build();
        return cache.asMap();
    }

    /**
     * Create a time based eviction cache in which an entry's age is determined on a last-write basis.
     *
     * @param ageInSeconds how old, in seconds, a cache entry must be to be evicted after last write
     * @return a time based concurrent cache
     */
    @Override
    public <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        Cache<K, V> cache = Caffeine.newBuilder().initialCapacity(32).expireAfterWrite(ageInSeconds, TimeUnit.SECONDS).executor(Runnable::run).build();
        return cache.asMap();
    }
}
