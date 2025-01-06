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
}
