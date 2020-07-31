/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

/**
 * This replaces the need for an inverse cache in {@link BoundedConcurrentCache} to look up a key from a value
 * 
 * @param <K> the type of the corresponding key
 */
public interface CacheValue<K> {

    /**
     * Returns the corresponding cache key for this value.
     * 
     * @return cache key
     */
    K getKey();

}
