/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.newrelic.agent.bridge.CollectionFactory;

public class AgentCollectionFactory implements CollectionFactory {

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        Cache<K, V> cache = CacheBuilder.newBuilder().concurrencyLevel(32).weakKeys().build();
        return cache.asMap();
    }
}
