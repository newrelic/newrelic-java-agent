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

public class DefaultCollectionFactory implements CollectionFactory {

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        return Collections.synchronizedMap(new WeakHashMap<K, V>());
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
        return k -> map.computeIfAbsent(k, k1 -> {
            if (map.size() >= maxSize) {
                map.remove(map.keySet().iterator().next());
            }
            return loader.apply(k1);
        });
    }

    /**
     * Note: In this implementation, this method will return the loader function as is.
     */
    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        return loader;
    }
}
