/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.bridge.CollectionFactory;

public class AgentCollectionFactory implements CollectionFactory {

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        Cache<K, V> cache = Caffeine.newBuilder().initialCapacity(32).weakKeys().executor(Runnable::run).build();
        return cache.asMap();
    }
}
