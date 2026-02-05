/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.bridge;

/**
 * Listener for cache entry removals.
 */
public interface CacheRemovalListener<K, V> {

    /**
     * Possible reasons an entry was removed from a cache.
     */
    enum RemovalReason {
        EXPIRED,
        EXPLICIT,
        SIZE,
        REPLACED,
        COLLECTED
    }

    /**
     * Callback executed when an entry is removed from a cache
     *
     * @param key the key that was removed (maybe null)
     * @param value the value that was removed (maybe null)
     * @param reason why the entry was removed
     */
    void onRemoval(K key, V value, RemovalReason reason);
}
