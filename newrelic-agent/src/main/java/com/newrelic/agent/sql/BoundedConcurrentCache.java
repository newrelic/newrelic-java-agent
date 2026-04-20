/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.bridge.AgentBridge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * 
 * Bounded concurrent cache that stores key value pairs (K,V).
 * 
 * When the cache reaches its maximum capacity, any new item added replaces the largest item in the cache.
 * 
 * To update a value in the cache, call <tt>putReplace</tt>.
 * 
 * This cache is thread-safe, but the behavior is not 100% deterministic and may end up returning slightly less
 * than the size limit in order to boost performance. However, it will never return a list that is over the limit. 
 * 
 * @param <K> Key type
 * @param <V> Value type
 */
public class BoundedConcurrentCache<K, V extends Comparable<V> & CacheValue<K>> {
    private final int maxCapacity;
    private final PriorityBlockingQueue<V> priorityQueue;
    private final Map<K, V> cache;

    public BoundedConcurrentCache(int size) {
        this(size, null);
    }

    public BoundedConcurrentCache(int size, Comparator<V> comparator) {
        this.maxCapacity = size;
        this.priorityQueue = new PriorityBlockingQueue<>(size, comparator);
        cache = AgentBridge.collectionFactory.createCacheWithInitialCapacity(16);
    }

    public V get(K sql) {
        return cache.get(sql);
    }

    public V putIfAbsent(K key, V value) {
        V putValue = cache.putIfAbsent(key, value);
        if (putValue != null) {
            return putValue;
        }
        priorityQueue.add(value);

        // replace min if cache gets full
        while (priorityQueue.size() > maxCapacity) {
            V val = priorityQueue.poll();
            K sqlToRemove = val.getKey();
            cache.remove(sqlToRemove);
        }
        return null;
    }

    /**
     * Inserts and replaces value in the cache. This method should be called whenever a value is modified.
     * 
     * @param key key of value to update.
     */
    public void putReplace(K key, V value) {
        V valueToRemove = cache.get(key);
        if (valueToRemove != null) {
            cache.remove(key);
            priorityQueue.remove(valueToRemove);
        }

        putIfAbsent(key, value);
    }

    public int size() {
        return priorityQueue.size();
    }

    public void clear() {
        cache.clear();
        priorityQueue.clear();
    }

    public List<V> asList() {
        return new ArrayList<>(priorityQueue);
    }
}
