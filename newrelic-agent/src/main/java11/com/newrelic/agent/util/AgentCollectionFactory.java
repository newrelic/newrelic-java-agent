/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.newrelic.agent.bridge.CacheRemovalListener;
import com.newrelic.agent.bridge.CleanableMap;
import com.newrelic.agent.bridge.CollectionFactory;
import com.newrelic.agent.config.JavaVersionUtils;

/**
 * This is the main instrumentation of CollectionFactory which is used when the agent is loaded.
 * It delegates to either Caffeine 2.9.3 (Java 8-10) or Caffeine 3.2.3 (Java 11+) based on the
 * detected Java version at runtime.
 */
public class AgentCollectionFactory implements CollectionFactory {

    private static final CollectionFactory DELEGATE = new Caffeine3CollectionFactory();

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        return DELEGATE.createConcurrentWeakKeyedMap();
    }

    @Override
    public <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        return DELEGATE.createConcurrentTimeBasedEvictionMap(ageInSeconds);
    }

    @Override
    public <K, V> Map<K, V> createConcurrentAccessTimeBasedEvictionMap(long ageInSeconds, int initialCapacity) {
        return DELEGATE.createConcurrentAccessTimeBasedEvictionMap(ageInSeconds, initialCapacity);
    }

    @Override
    public <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize) {
        return DELEGATE.memorize(loader, maxSize);
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        return DELEGATE.createAccessTimeBasedCache(ageInSeconds, initialCapacity, loader);
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCacheWithMaxSize(long ageInSeconds, int maxSize, Function<K, V> loader) {
        return DELEGATE.createAccessTimeBasedCacheWithMaxSize(ageInSeconds, maxSize, loader);
    }

    @Override
    public <K, V> Function<K, V> createLoadingCache(Function<K, V> loader) {
        return DELEGATE.createLoadingCache(loader);
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize) {
        return DELEGATE.createCacheWithWeakKeysAndSize(maxSize);
    }

    @Override
    public <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity) {
        return DELEGATE.createWeakKeyedCacheWithInitialCapacity(initialCapacity);
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize) {
        return DELEGATE.createCacheWithWeakKeysInitialCapacityAndSize(initialCapacity, maxSize);
    }

    @Override
    public <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity) {
        return DELEGATE.createCacheWithInitialCapacity(initialCapacity);
    }

    @Override
    public <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader) {
        return DELEGATE.createWeakKeyedLoadingCacheWithInitialCapacity(initialCapacity, loader);
    }

    @Override
    public <K, V> CleanableMap<K, V> createCacheWithWriteExpirationAndRemovalListener(
            long age,
            TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener) {
        return DELEGATE.createCacheWithWriteExpirationAndRemovalListener(age, unit, initialCapacity, listener);
    }

    @Override
    public <K, V> CleanableMap<K, V> createCacheWithAccessExpirationAndRemovalListener(
            long age,
            TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener) {
        return DELEGATE.createCacheWithAccessExpirationAndRemovalListener(age, unit, initialCapacity, listener);
    }
}
