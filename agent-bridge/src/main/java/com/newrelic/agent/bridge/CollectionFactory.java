/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.Map;
import java.util.function.Function;

/**
 * Allows instrumentation and bridge API implementations to use collections from third partly libraries without
 * depending directly on them.
 *
 * For example, there is no default weak-keyed concurrent map in the JDK, so instrumentation authors end up using a
 * fully synchronized wrapper around a {@link java.util.WeakHashMap}. Caffeine has a better implementation; this interface
 * allows the Agent to provide it at runtime.
 */
public interface CollectionFactory {

    /**
     * Create a concurrent-safe, weak-keyed map.
     * 
     * @param <K> Key type
     * @param <V> Value type
     * @return concurrent-safe, weak-keyed map
     */
    <K, V> Map<K, V> createConcurrentWeakKeyedMap();

    /**
     * Create a time based eviction cache in which an entry's age is determined on a last-write basis.
     *
     * @param ageInSeconds how old, in seconds, a cache entry must be to be evicted after last write
     * @return a time based concurrent cache
     *
     * @param <K> key type
     * @param <V> value type
     */
    <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds);

    /**
     * Create a time based eviction cache in which an entry's age is determined on a last-access basis.
     * Entries expire after the specified time has passed since the last read or write.
     *
     * @param ageInSeconds how old, in seconds, a cache entry must be to be evicted after last access
     * @param initialCapacity initial capacity to pre-allocate
     * @return a time based concurrent cache with access-based expiration
     *
     * @param <K> key type
     * @param <V> value type
     */
    <K, V> Map<K, V> createConcurrentAccessTimeBasedEvictionMap(long ageInSeconds, int initialCapacity);

    /**
     * Wraps the provided function into one that will cache the results for future calls.
     * @param loader the function that calculates the value.
     * @param maxSize the max number of items to be cached.
     * @return the cached item, or the result of the loader call.
     * @param <K> the type of key
     * @param <V> the type of value stored/returned
     */
    <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize);

    /**
     * Create a time based eviction cache in which an entry's age is determined on a last-access basis.
     *
     * @param <K>             key type
     * @param <V>             cached type
     * @param ageInSeconds    how old, in seconds, a cache entry must be to be evicted after last access
     * @param initialCapacity the initial capacity of the cache
     * @param loader          the function to calculate the value for a key, used if the key is not cached
     * @return a time based concurrent cache
     */
    <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader);

    /**
     * Create a time based eviction cache with maximum size and access-based expiration.
     * Combines size-bounded eviction with time-based expiration for optimal memory management.
     *
     * @param <K>          key type
     * @param <V>          cached type
     * @param ageInSeconds how old, in seconds, a cache entry must be to be evicted after last access
     * @param maxSize      maximum number of entries before size-based eviction
     * @param loader       the function to calculate the value for a key, used if the key is not cached
     * @return a function that caches results with both size and time limits
     */
    <K, V> Function<K, V> createAccessTimeBasedCacheWithMaxSize(long ageInSeconds, int maxSize, Function<K, V> loader);

    /**
     * Create a loading cache that computes values on demand using the provided loader function.
     *
     * @param <K>    key type
     * @param <V>    cached type
     * @param loader the function to compute values for cache misses
     * @return a function that caches results
     */
    <K, V> Function<K, V> createLoadingCache(Function<K, V> loader);

    /**
     * Create a cache with weak keys and a maximum size.
     * Used for caching with automatic cleanup when keys are garbage collected.
     *
     * @param <K>     key type
     * @param <V>     cached type
     * @param maxSize maximum number of entries before eviction
     * @return a map-backed cache with weak keys and size limit
     */
    <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize);

    /**
     * Create a cache with weak keys and initial capacity.
     * Used when weak key cleanup is needed with a known initial size.
     *
     * @param <K>             key type
     * @param <V>             cached type
     * @param initialCapacity initial capacity to pre-allocate
     * @return a map-backed cache with weak keys
     */
    <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity);

    /**
     * Create a cache with weak keys, initial capacity, and maximum size.
     * Combines weak key cleanup with size bounds and initial capacity.
     *
     * @param <K>             key type
     * @param <V>             cached type
     * @param initialCapacity initial capacity to pre-allocate
     * @param maxSize         maximum number of entries before eviction
     * @return a map-backed cache with weak keys, initial capacity, and size limit
     */
    <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize);

    /**
     * Create a cache with initial capacity only.
     * Used for simple caching without expiration or weak keys.
     *
     * @param <K>             key type
     * @param <V>             cached type
     * @param initialCapacity initial capacity to pre-allocate
     * @return a map-backed cache
     */
    <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity);

    /**
     * Create a loading cache with weak keys and initial capacity.
     * Combines weak key cleanup with automatic value loading.
     *
     * @param <K>             key type
     * @param <V>             cached type
     * @param initialCapacity initial capacity to pre-allocate
     * @param loader          the function to compute values for cache misses
     * @return a function that caches results with weak keys
     */
    <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader);

    /**
     * Create a cache with time-based eviction (write) and removal listener.
     * Entries expire after the specified time has passed since the last write.
     * The listener is invoked when entries are removed for any reason (expiration, explicit removal, etc).
     *
     * @param <K>             key type
     * @param <V>             cached type
     * @param age             how old a cache entry must be to be evicted after last write
     * @param unit            time unit for the age parameter
     * @param initialCapacity initial capacity to pre-allocate
     * @param listener        callback invoked when entries are removed
     * @return a cleanable map with expiration and removal tracking
     */
    <K, V> CleanableMap<K, V> createCacheWithWriteExpirationAndRemovalListener(
            long age,
            java.util.concurrent.TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener);

    /**
     * Create a cache with time-based eviction (access) and removal listener.
     * Entries expire after the specified time has passed since the last read or write.
     * The listener is invoked when entries are removed for any reason (expiration, explicit removal, etc).
     *
     * @param <K>             key type
     * @param <V>             cached type
     * @param age             how old a cache entry must be to be evicted after last access
     * @param unit            time unit for the age parameter
     * @param initialCapacity initial capacity to pre-allocate
     * @param listener        callback invoked when entries are removed
     * @return a cleanable map with expiration and removal tracking
     */
    <K, V> CleanableMap<K, V> createCacheWithAccessExpirationAndRemovalListener(
            long age,
            java.util.concurrent.TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener);
}
