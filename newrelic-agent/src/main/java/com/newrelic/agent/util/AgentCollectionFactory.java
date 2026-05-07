/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.CacheRemovalListener;
import com.newrelic.agent.bridge.CleanableMap;
import com.newrelic.agent.bridge.CollectionFactory;
import com.newrelic.agent.config.JavaVersionUtils;

/**
 * This is the main implementation of CollectionFactory which is used when the agent is loaded.
 * It delegates to either Caffeine 2.9.3 (Java 8-10) or Caffeine 3.2.3 (Java 11+) based on the
 * detected Java version at runtime.
 * <p>
 * Uses reflection to load the appropriate factory to avoid class loading issues:
 * - Caffeine 2 uses sun.misc.Unsafe (removed in Java 26+)
 * - Caffeine 3 uses VarHandle (unavailable before Java 11)
 * <p>
 * A config exists (newrelic.config.collectionfactory.forcev2 or NEW_RELIC_COLLECTIONFACTORY_FORCEV2 env variable) can be used
 * to force the caffeine version 2 factory if version 3 is not usable.
 */
public class AgentCollectionFactory implements CollectionFactory {

    private static final String FORCE_V2_FACTORY_SYSPROP_CONFIG = "newrelic.config.collectionfactory.forcev2";
    private static final String FORCE_V2_FACTORY_ENV_CONFIG = "NEW_RELIC_COLLECTIONFACTORY_FORCEV2";

    private static volatile CollectionFactory DELEGATE;

    private static CollectionFactory getDelegate() {
        // Double-checked locking for lazy initialization
        if (DELEGATE == null) {
            synchronized (AgentCollectionFactory.class) {
                if (DELEGATE == null) {
                    DELEGATE = createDelegate();
                }
            }
        }
        return DELEGATE;
    }

    private static CollectionFactory createDelegate() {
        int javaVersion = JavaVersionUtils.getMajorVersion();

        // Java 8-10: Use Caffeine 2 (uses sun.misc.Unsafe, available in Java 8-25)
        // Java 11+: Use Caffeine 3 (uses VarHandle, Unsafe removed in Java 26)
        // Also, use Caffeine 2 if the appropriate config flag is set to "true"
        if ((javaVersion >= 8 && javaVersion <= 10) || getForceVersion2Config()) {
            CollectionFactory factory = tryLoadFactory("com.newrelic.agent.util.Caffeine2CollectionFactory");
            if (factory != null) {
                Agent.LOG.info("AgentCollectionFactory: Using Caffeine 2.x for Java " + javaVersion);
                return factory;
            }
        } else if (javaVersion >= 11) {
            CollectionFactory factory = tryLoadFactory("com.newrelic.agent.util.Caffeine3CollectionFactory");
            if (factory != null) {
                Agent.LOG.info("AgentCollectionFactory: Using Caffeine 3.x for Java " + javaVersion);
                return factory;
            }
        }

        throw new RuntimeException("Failed to load any Caffeine CollectionFactory implementation for Java " + javaVersion);
    }

    private static boolean getForceVersion2Config() {
        String sysVal = System.getProperty(FORCE_V2_FACTORY_SYSPROP_CONFIG);
        String envVal = System.getenv(FORCE_V2_FACTORY_ENV_CONFIG);

        return Boolean.parseBoolean(sysVal) || Boolean.parseBoolean(envVal);
    }

    private static CollectionFactory tryLoadFactory(String className) {
        try {
            Class<?> factoryClass = Class.forName(className);
            return (CollectionFactory) factoryClass.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        return getDelegate().createConcurrentWeakKeyedMap();
    }

    @Override
    public <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        return getDelegate().createConcurrentTimeBasedEvictionMap(ageInSeconds);
    }

    @Override
    public <K, V> Map<K, V> createConcurrentAccessTimeBasedEvictionMap(long ageInSeconds, int initialCapacity) {
        return getDelegate().createConcurrentAccessTimeBasedEvictionMap(ageInSeconds, initialCapacity);
    }

    @Override
    public <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize) {
        return getDelegate().memorize(loader, maxSize);
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        return getDelegate().createAccessTimeBasedCache(ageInSeconds, initialCapacity, loader);
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCacheWithMaxSize(long ageInSeconds, int maxSize, Function<K, V> loader) {
        return getDelegate().createAccessTimeBasedCacheWithMaxSize(ageInSeconds, maxSize, loader);
    }

    @Override
    public <K, V> Function<K, V> createLoadingCache(Function<K, V> loader) {
        return getDelegate().createLoadingCache(loader);
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize) {
        return getDelegate().createCacheWithWeakKeysAndSize(maxSize);
    }

    @Override
    public <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity) {
        return getDelegate().createWeakKeyedCacheWithInitialCapacity(initialCapacity);
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize) {
        return getDelegate().createCacheWithWeakKeysInitialCapacityAndSize(initialCapacity, maxSize);
    }

    @Override
    public <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity) {
        return getDelegate().createCacheWithInitialCapacity(initialCapacity);
    }

    @Override
    public <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader) {
        return getDelegate().createWeakKeyedLoadingCacheWithInitialCapacity(initialCapacity, loader);
    }

    @Override
    public <K, V> CleanableMap<K, V> createCacheWithWriteExpirationAndRemovalListener(
            long age,
            TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener) {
        return getDelegate().createCacheWithWriteExpirationAndRemovalListener(age, unit, initialCapacity, listener);
    }

    @Override
    public <K, V> CleanableMap<K, V> createCacheWithAccessExpirationAndRemovalListener(
            long age,
            TimeUnit unit,
            int initialCapacity,
            CacheRemovalListener<K, V> listener) {
        return getDelegate().createCacheWithAccessExpirationAndRemovalListener(age, unit, initialCapacity, listener);
    }
}