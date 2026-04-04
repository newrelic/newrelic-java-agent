/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.extension;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExtensionHolder;
import com.newrelic.agent.bridge.ExtensionHolderFactory;

import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Implementation for creating an {@link ExtensionHolder}
 * 
 * @see ExtensionHolderImpl
 */
public class ExtensionHolderFactoryImpl implements ExtensionHolderFactory {
    @Override
    public <T> ExtensionHolder<T> build() {
        return new ExtensionHolderImpl<>();
    }

    /**
     * Uses a caffeine cache to store weaver extension classes.
     */
    public static class ExtensionHolderImpl<T> implements ExtensionHolder<T> {
        // @formatter:off
        private final Map<Object, T> instanceCache = AgentBridge.collectionFactory.createWeakKeyedCacheWithInitialCapacity(32);
        // @formatter:on

        @Override
        public T getAndRemoveExtension(Object instance) {
            return instanceCache.remove(instance);
        }

        /**
         * Uses putIfAbsent pattern instead of computeIfAbsent() to avoid
         * ConcurrentHashMap bin-level locking during value creation.
         * This prevents contention issues where threads get stuck in
         * helpTransfer() under high concurrency.
         *
         * Trade-off: Under race conditions, multiple threads may create
         * extension instances, but only one wins (via putIfAbsent).
         * This is safe because valueLoader creates empty instances -
         * no pre-populated state is lost.
         */
        @Override
        public T getExtension(Object instance, Supplier<T> valueLoader) {
            try {
                // Fast path: already exists
                T value = instanceCache.get(instance);
                if (value != null) {
                    return value;
                }

                // Create value OUTSIDE of any lock - avoids computeIfAbsent() bin-level contention
                T newValue = valueLoader.get();

                // Atomic put - only one thread wins, losers get the winner's value
                T existing = instanceCache.putIfAbsent(instance, newValue);
                return existing != null ? existing : newValue;
            } catch (RuntimeException e) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Unable to load extension class for {0}",
                        instance.getClass().getName());
                throw e;
            }
        }
    }
}
