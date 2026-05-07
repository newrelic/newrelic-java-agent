/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.Map;

/**
 * A Map that supports explicit cleanup/maintenance operations.
 * This is primarily used for cache implementations that may defer maintenance
 * operations and need periodic explicit cleanup.
 */
public interface CleanableMap<K, V> extends Map<K, V> {

    /**
     * Perform any pending maintenance operations, such as:
     * - Evicting expired entries
     * - Invoking removal listeners for pending removals
     * - Performing size-based evictions
     * - Other deferred maintenance work
     * <p>
     * Implementations may perform these operations automatically during
     * normal map operations, but this method allows explicit triggering
     * of maintenance when needed (e.g., during periodic cleanup cycles).
     */
    void cleanUp();
}