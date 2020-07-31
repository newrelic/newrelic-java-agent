/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.MapMaker;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Map implementation that performs lazy initialization of the actual map.
 *
 * Because this class relies on Guava's MapMaker, it does not allow null key or values.
 *
 * This class is thread safe.
 */
public class LazyMapImpl<K, V> implements Map<K, V> {

    private final AtomicReference<Map<K, V>> parameters = new AtomicReference<>();
    private final MapMaker factory;

    /**
     * Create a lazy map with a small initial size, concurrently accessible by only 1 thread.
     */
    public LazyMapImpl() {
        this(5);
    }

    /**
     * Create a lazy map with a given initial size, concurrently accessible by only 1 thread.
     */
    public LazyMapImpl(int initialSize) {
        this(new MapMaker().initialCapacity(initialSize).concurrencyLevel(1));
    }

    /**
     * Create a lazy map of your own design
     *
     * @param factory a MapMaker, preconfigured to construct the desired map.
     */
    public LazyMapImpl(MapMaker factory) {
        this.factory = factory;
    }

    /**
     * Get or create the parameters Map.
     *
     * @return the parameters
     */
    private Map<K, V> getParameters() {
        if (parameters.get() == null) {
            parameters.compareAndSet(null, factory.<K, V>makeMap());
        }
        return parameters.get();
    }

    /**
     * Put a parameter.
     *
     * @return the previous parameter value (null if it did not exist)
     */
    @Override
    public V put(K key, V value) {
        return getParameters().put(key, value);
    }

    /**
     * Copy parameters from the given map.
     *
     * @param params parameters to be copied
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> params) {
        if (params != null && !params.isEmpty()) {
            getParameters().putAll(params);
        }
    }

    /**
     * Remove a parameter.
     *
     * @return the previous parameter value (null if it did not exist)
     */
    @Override
    public V remove(Object key) {
        if (parameters.get() == null) {
            return null;
        }
        return getParameters().remove(key);
    }

    /**
     * Get a parameter.
     *
     * @return the parameter value (null if it did not exist)
     */
    @Override
    public V get(Object key) {
        if (parameters.get() == null) {
            return null;
        }
        return getParameters().get(key);
    }

    /**
     * Clear the parameters.
     */
    @Override
    public void clear() {
        if (parameters.get() != null) {
            parameters.get().clear();
        }
    }

    @Override
    public int size() {
        if (parameters.get() == null) {
            return 0;
        }
        return getParameters().size();
    }

    @Override
    public boolean isEmpty() {
        if (parameters.get() == null) {
            return true;
        }
        return getParameters().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (parameters.get() == null) {
            return false;
        }
        return getParameters().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (parameters.get() == null) {
            return false;
        }
        return getParameters().containsValue(value);
    }

    @Override
    public Set<K> keySet() {
        // Do not attempt to optimized this method
        // by returning Collections.emptySet(). The
        // caller is allowed to hold the key set
        // indefinitely, so it must refer to the
        // actual collection, which means we must
        // create it. JAVA-932.
        // if (parameters.get() == null) {
        // return Collections.emptySet();
        // }
        return getParameters().keySet();
    }

    @Override
    public Collection<V> values() {
        // Do not attempt to optimized this method
        // by returning Collections.emptySet(). The
        // caller is allowed to hold the key set
        // indefinitely, so it must refer to the
        // actual collection, which means we must
        // create it. JAVA-932.
        // if (parameters.get() == null) {
        // return Collections.emptySet();
        // }
        return getParameters().values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        // Do not attempt to optimized this method
        // by returning Collections.emptySet(). The
        // caller is allowed to hold the key set
        // indefinitely, so it must refer to the
        // actual collection, which means we must
        // create it. JAVA-932.
        // if (parameters.get() == null) {
        // return Collections.emptySet();
        // }
        return getParameters().entrySet();
    }

}
