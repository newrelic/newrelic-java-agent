/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.Map;

/**
 * Allows instrumentation and bridge API implementations to use collections from third partly libraries without
 * depending directly on them.
 *
 * For example, there is no default weak-keyed concurrent map in the JDK, so instrumentation authors end up using a
 * fully synchronized wrapper around a {@link java.util.WeakHashMap}. Guava has a more good implementation; this class
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
}
