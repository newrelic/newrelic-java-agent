/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

// Import from SHADED Caffeine 2.9.3
import com.newrelic.agent.bridge.CacheRemovalListener;
import com.newrelic.agent.bridge.CleanableMap;
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Cache;
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.LoadingCache;
import com.newrelic.agent.bridge.CollectionFactory;

/**
 * CollectionFactory implementation using Caffeine 2.9.3.
 * Used for Java 8-10.
 */
public class Caffeine2CollectionFactory implements CollectionFactory {

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

    /**
     * Create a time based eviction cache in which an entry's age is determined on a last-access basis.
     *
     * @param ageInSeconds how old, in seconds, a cache entry must be to be evicted after last access
     * @param initialCapacity initial capacity to pre-allocate
     * @return a time based concurrent cache with access-based expiration
     */
    @Override
    public <K, V> Map<K, V> createConcurrentAccessTimeBasedEvictionMap(long ageInSeconds, int initialCapacity) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .expireAfterAccess(ageInSeconds, TimeUnit.SECONDS)
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
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .expireAfterAccess(ageInSeconds, TimeUnit.SECONDS)
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCacheWithMaxSize(long ageInSeconds, int maxSize, Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(ageInSeconds, TimeUnit.SECONDS)
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

    @Override
    public <K, V> CleanableMap<K, V> createCacheWithWriteExpirationAndRemovalListener(
            long age,
            TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .expireAfterWrite(age, unit)
                .executor(Runnable::run)
                .removalListener((K key, V value, com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.RemovalCause cause) -> {
                    listener.onRemoval(key, value, convertRemovalCause(cause));
                })
                .build();
        return new CaffeineCleanableMap<>(cache);
    }

    @Override
    public <K, V> CleanableMap<K, V> createCacheWithAccessExpirationAndRemovalListener(
            long age,
            TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .expireAfterAccess(age, unit)
                .executor(Runnable::run)
                .removalListener((K key, V value, com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.RemovalCause cause) -> {
                    listener.onRemoval(key, value, convertRemovalCause(cause));
                })
                .build();
        return new CaffeineCleanableMap<>(cache);
    }

    private CacheRemovalListener.RemovalReason convertRemovalCause(
            com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.RemovalCause cause) {
        switch (cause) {
            case EXPIRED:
                return CacheRemovalListener.RemovalReason.EXPIRED;
            case SIZE:
                return CacheRemovalListener.RemovalReason.SIZE;
            case REPLACED:
                return CacheRemovalListener.RemovalReason.REPLACED;
            case COLLECTED:
                return CacheRemovalListener.RemovalReason.COLLECTED;
            default:
                return CacheRemovalListener.RemovalReason.EXPLICIT;
        }
    }

    /**
     * Wrapper that exposes Caffeine cache as CleanableMap, allowing cleanUp() to be called.
     */
    private static class CaffeineCleanableMap<K, V> implements CleanableMap<K, V> {
        private final Cache<K, V> cache;
        private final Map<K, V> map;

        CaffeineCleanableMap(Cache<K, V> cache) {
            this.cache = cache;
            this.map = cache.asMap();
        }

        @Override
        public void cleanUp() {
            cache.cleanUp();
        }

        // Delegate all Map methods to the cache's map view
        @Override
        public int size() {
            return map.size();
        }
        @Override public boolean isEmpty() { return map.isEmpty(); }
        @Override public boolean containsKey(Object key) { return map.containsKey(key); }
        @Override public boolean containsValue(Object value) { return map.containsValue(value); }
        @Override public V get(Object key) { return map.get(key); }
        @Override public V put(K key, V value) { return map.put(key, value); }
        @Override public V remove(Object key) { return map.remove(key); }
        @Override public void putAll(Map<? extends K, ? extends V> m) { map.putAll(m); }
        @Override public void clear() { map.clear(); }
        @Override public java.util.Set<K> keySet() { return map.keySet(); }
        @Override public java.util.Collection<V> values() { return map.values(); }
        @Override public java.util.Set<Entry<K, V>> entrySet() { return map.entrySet(); }
    }
}
