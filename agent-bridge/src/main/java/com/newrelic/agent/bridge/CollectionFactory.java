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
     * Wraps the provided function into one that will cache the results for future calls.
     * @param loader the function that calculates the value.
     * @param maxSize the max number of items to be cached.
     * @return the cached item, or the result of the loader call.
     * @param <K> the type of key
     * @param <V> the type of value stored/returned
     */
    <K, V> Function<K, V> memoize(Function<K, V> loader, int maxSize);

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
}
