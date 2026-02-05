/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * This implementation of {@link CollectionFactory} will only be used if the agent-bridge
 * is being used by an application and the agent is NOT being loaded. Thus, it is unlikely
 * that the objects created by this implementation are going to receive much use.
 * So methods in this implementation do not need to implement all functional requirements
 * of the methods in the interface, but they should not break under low use.
 */
public class DefaultCollectionFactory implements CollectionFactory {

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        return Collections.synchronizedMap(new WeakHashMap<>());
    }

    /**
     * Note: In this implementation, this method will return a simple concurrent map since an eviction
     * cache can't be easily created with just vanilla JDK Map SDKs.
     *
     * @param ageInSeconds how old, in seconds, a cache entry must be to be evicted after last write
     * @return a time based concurrent cache
     */
    @Override
    public <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        return Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Note: In this implementation, this method will return a simple synchronized map since an access-based
     * eviction cache can't be easily created with just vanilla JDK Map SDKs.
     * Both ageInSeconds and initialCapacity parameters are ignored.
     *
     * @param ageInSeconds how old, in seconds, a cache entry must be to be evicted after last access
     * @param initialCapacity initial capacity to pre-allocate
     * @return a time based concurrent cache
     */
    @Override
    public <K, V> Map<K, V> createConcurrentAccessTimeBasedEvictionMap(long ageInSeconds, int initialCapacity) {
        return Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    public <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize) {
        Map<K, V> map = new ConcurrentHashMap<>();

        return k -> {
            if (map.size() >= maxSize) {
                V value = map.get(k);
                return value == null ? loader.apply(k) : value;
            }
            return map.computeIfAbsent(k, loader);
        };
    }

    /**
     * Note: In this implementation, this method will return the loader function as is.
     */
    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        return loader;
    }

    /**
     * Note: In this implementation, this method will return the loader function as is.
     * Both ageInSeconds and maxSize parameters are ignored.
     */
    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCacheWithMaxSize(long ageInSeconds, int maxSize, Function<K, V> loader) {
        return loader;
    }

    /**
     * Note: In this implementation, this method will return the loader function as is.
     */
    @Override
    public <K, V> Function<K, V> createLoadingCache(Function<K, V> loader) {
        return loader;
    }

    /**
     * Note: In this implementation, this method will return a synchronized weak hash map.
     * The maxSize parameter is ignored since JDK WeakHashMap doesn't support size limits.
     */
    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize) {
        return Collections.synchronizedMap(new WeakHashMap<>());
    }

    /**
     * Note: In this implementation, this method will return a synchronized weak hash map.
     * The initialCapacity parameter is ignored.
     */
    @Override
    public <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity) {
        return Collections.synchronizedMap(new WeakHashMap<>());
    }

    /**
     * Note: In this implementation, this method will return a synchronized weak hash map.
     * Both initialCapacity and maxSize parameters are ignored.
     */
    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize) {
        return Collections.synchronizedMap(new WeakHashMap<>());
    }

    /**
     * Note: In this implementation, this method will return a synchronized hash map.
     * The initialCapacity parameter is ignored.
     */
    @Override
    public <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity) {
        return Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Note: In this implementation, this method will return the loader function as is.
     * The initialCapacity parameter is ignored and weak keys are not supported.
     */
    @Override
    public <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader) {
        return loader;
    }

    /**
     * Note: In this implementation, this method will return a simple synchronized map since time-based
     * eviction and removal listeners can't be easily created with just vanilla JDK Map SDKs.
     * All parameters (age, unit, initialCapacity, listener) are ignored.
     * The cleanUp() method is a no-op since there's no deferred maintenance.
     */
    @Override
    public <K, V> CleanableMap<K, V> createCacheWithWriteExpirationAndRemovalListener(
            long age,
            java.util.concurrent.TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener) {
        return new DefaultCleanableMap<>(Collections.synchronizedMap(new HashMap<>()));
    }

    /**
     * Note: In this implementation, this method will return a simple synchronized map since time-based
     * eviction and removal listeners can't be easily created with just vanilla JDK Map SDKs.
     * All parameters (age, unit, initialCapacity, listener) are ignored.
     * The cleanUp() method is a no-op since there's no deferred maintenance.
     */
    @Override
    public <K, V> CleanableMap<K, V> createCacheWithAccessExpirationAndRemovalListener(
            long age,
            java.util.concurrent.TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener) {
        return new DefaultCleanableMap<>(Collections.synchronizedMap(new HashMap<>()));
    }

    /**
     * Simple wrapper that delegates all Map operations and provides a no-op cleanUp().
     */
    private static class DefaultCleanableMap<K, V> implements CleanableMap<K, V> {
        private final Map<K, V> delegate;

        DefaultCleanableMap(Map<K, V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void cleanUp() {
            // No-op for default implementation
        }

        // Delegate all Map methods
        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean containsKey(Object key) { return delegate.containsKey(key); }
        @Override public boolean containsValue(Object value) { return delegate.containsValue(value); }
        @Override public V get(Object key) { return delegate.get(key); }
        @Override public V put(K key, V value) { return delegate.put(key, value); }
        @Override public V remove(Object key) { return delegate.remove(key); }
        @Override public void putAll(Map<? extends K, ? extends V> m) { delegate.putAll(m); }
        @Override public void clear() { delegate.clear(); }
        @Override public java.util.Set<K> keySet() { return delegate.keySet(); }
        @Override public java.util.Collection<V> values() { return delegate.values(); }
        @Override public java.util.Set<Entry<K, V>> entrySet() { return delegate.entrySet(); }
    }
}
