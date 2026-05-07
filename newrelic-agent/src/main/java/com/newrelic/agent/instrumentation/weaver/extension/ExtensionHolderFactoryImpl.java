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

        @Override
        public T getExtension(Object instance, Supplier<T> valueLoader) {
            try {
                return instanceCache.computeIfAbsent(instance, k -> valueLoader.get());
            } catch (RuntimeException e) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Unable to load extension class for {0}",
                        instance.getClass().getName());
                throw e;
            }
        }
    }
}
