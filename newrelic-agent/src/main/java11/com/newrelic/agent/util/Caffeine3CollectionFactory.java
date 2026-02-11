/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

// Import from SHADED Caffeine 3.2.3
import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.Cache;
import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.LoadingCache;
import com.newrelic.agent.bridge.CollectionFactory;

/**
 * CollectionFactory implementation using Caffeine 3.2.3.
 * Used for Java 11+. This version uses VarHandle instead of sun.misc.Unsafe.
 */
public class Caffeine3CollectionFactory implements CollectionFactory {

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
        // Note: Caffeine 3.x uses Duration instead of TimeUnit
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(32)
                .expireAfterWrite(Duration.ofSeconds(ageInSeconds))
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    /**
     * Create a time based eviction cache in which an entry's age is determined on a last-access basis.
     *
     * @param ageInSeconds how old, in seconds, a cache entry must be to be evicted after last access
     * @param initialCapacity initial capacity to pre-allocate
     * @return a time based concurrent cache with access-based expiration
     */
    @Override
    public <K, V> Map<K, V> createConcurrentAccessTimeBasedEvictionMap(long ageInSeconds, int initialCapacity) {
        // Note: Caffeine 3.x uses Duration instead of TimeUnit
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .expireAfterAccess(Duration.ofSeconds(ageInSeconds))
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        // Note: Caffeine 3.x uses Duration instead of TimeUnit
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .expireAfterAccess(Duration.ofSeconds(ageInSeconds))
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCacheWithMaxSize(long ageInSeconds, int maxSize, Function<K, V> loader) {
        // Note: Caffeine 3.x uses Duration instead of TimeUnit
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(Duration.ofSeconds(ageInSeconds))
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Function<K, V> createLoadingCache(Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .weakKeys()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }
}
