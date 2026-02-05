/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;
import java.util.function.Function;

import com.newrelic.agent.bridge.CollectionFactory;
import com.newrelic.agent.config.JavaVersionUtils;

/**
 * This is the main instrumentation of CollectionFactory which is used when the agent is loaded.
 * It delegates to either Caffeine 2.9.3 (Java 8-10) or Caffeine 3.2.3 (Java 11+) based on the
 * detected Java version at runtime.
 */
public class AgentCollectionFactory implements CollectionFactory {

    private static final CollectionFactory DELEGATE = new Caffeine3CollectionFactory();

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        return DELEGATE.createConcurrentWeakKeyedMap();
    }

    @Override
    public <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        return DELEGATE.createConcurrentTimeBasedEvictionMap(ageInSeconds);
    }

    @Override
    public <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize) {
        return DELEGATE.memorize(loader, maxSize);
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        return DELEGATE.createAccessTimeBasedCache(ageInSeconds, initialCapacity, loader);
    }
}
