/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.extension;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExtensionHolder;
import com.newrelic.agent.bridge.ExtensionHolderFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
     * Uses a guava cache to store weaver extension classes.
     */
    public static class ExtensionHolderImpl<T> implements ExtensionHolder<T> {
        // @formatter:off
        private final Cache<Object, T> instanceCache = CacheBuilder.newBuilder()
                .concurrencyLevel(32)
                .weakKeys()
                .build();
        // @formatter:on

        @Override
        public T getAndRemoveExtension(Object instance) {
            return instanceCache.asMap().remove(instance);
        }

        @Override
        public T getExtension(Object instance, Callable<T> valueLoader) {
            try {
                return instanceCache.get(instance, valueLoader);
            } catch (ExecutionException e) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Unable to load extension class for {0}",
                        instance.getClass().getName());
                throw new RuntimeException(e);
            }
        }
    }
}
