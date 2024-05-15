/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.Collections;
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
        return Collections.synchronizedMap(new WeakHashMap<K, V>());
    }

    @Override
    public <K, V> Function<K, V> memoize(Function<K, V> loader, int maxSize) {
        Map<K, V> map = new ConcurrentHashMap<>();
        return k -> map.computeIfAbsent(k, k1 -> {
            if (map.size() >= maxSize) {
                map.remove(map.keySet().iterator().next());
            }
            return loader.apply(k1);
        });
    }
}
