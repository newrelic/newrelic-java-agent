/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.weave.utils;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * A thread-safe cache with weak keys and LRU (Least Recently Used) eviction.
 * Assisted by AI
 *
 * <p>Features:
 * <ul>
 *   <li><b>Weak keys:</b> Keys can be garbage collected when no longer referenced externally</li>
 *   <li><b>Size limiting:</b> Evicts least-recently-used entries when max size is reached</li>
 *   <li><b>Thread-safe:</b> All operations are synchronized</li>
 * </ul>
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class WeakKeyLruCache<K, V> {
    private final int maxSize;

    /**
     * WeakHashMap handles automatic removal of entries when keys are garbage collected.
     * This is independent of LRU eviction.
     */
    private final Map<K, V> storage = new WeakHashMap<>();

    /**
     * Tracks access order for LRU eviction using WeakReferences.
     * Using WeakReferences prevents this list from holding strong references to keys,
     * which would defeat the purpose of WeakHashMap.
     */
    private final LinkedList<WeakReference<K>> accessOrder = new LinkedList<>();

    /**
     * Creates a new cache with the specified maximum size.
     *
     * @param maxSize Maximum number of entries before LRU eviction occurs
     * @throws IllegalArgumentException if maxSize is not positive
     */
    public WeakKeyLruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

    /**
     * Associates the specified value with the specified key.
     * If the cache previously contained a mapping for the key, the old value is replaced.
     * If the cache is at maximum size and the key is new, evicts the least recently used entry.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping
     * @throws NullPointerException if key is null
     */
    public synchronized V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }

        // Periodically clean up stale WeakReference objects
        if (accessOrder.size() > maxSize * 2) {
            cleanStaleReferences();
        }

        // Evict least recently used entry if at capacity and adding a new key
        if (storage.size() >= maxSize && !storage.containsKey(key)) {
            evictLeastRecentlyUsed();
        }

        V oldValue = storage.put(key, value);
        updateAccessOrder(key);
        return oldValue;
    }

    /**
     * Returns the value to which the specified key is mapped, or null if this cache
     * contains no mapping for the key. Updates the access order (moves key to most recent).
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null
     */
    public synchronized V get(K key) {
        if (key == null) {
            return null;
        }

        if (storage.containsKey(key)) {
            updateAccessOrder(key);
            return storage.get(key);
        }
        return null;
    }

    /**
     * Removes the mapping for a key from this cache if it is present.
     *
     * @param key key whose mapping is to be removed from the cache
     * @return the previous value associated with key, or null
     */
    public synchronized V remove(K key) {
        if (key == null) {
            return null;
        }

        V value = storage.remove(key);
        if (value != null) {
            removeFromAccessOrder(key);
        }
        return value;
    }

    /**
     * If the specified key is not already associated with a value, associates it with the given value.
     * This is equivalent to:
     * <pre>
     *   if (!cache.containsKey(key)) {
     *       cache.put(key, value);
     *       return null;
     *   } else {
     *       return cache.get(key);
     *   }
     * </pre>
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or null if there was no mapping
     * @throws NullPointerException if key is null
     */
    public synchronized V putIfAbsent(K key, V value) {
        if (storage.containsKey(key)) {
            updateAccessOrder(key);
            return storage.get(key);
        } else {
            put(key, value);
            return null;
        }
    }

    /**
     * Returns the number of entries currently in the cache.
     * Note: May include entries whose keys are about to be garbage collected.
     *
     * @return the number of entries in the cache
     */
    public synchronized int size() {
        return storage.size();
    }

    /**
     * Removes all entries from the cache.
     */
    public synchronized void clear() {
        storage.clear();
        accessOrder.clear();
    }

    /**
     * Returns true if this cache contains a mapping for the specified key.
     *
     * @param key key whose presence in this cache is to be tested
     * @return true if this cache contains a mapping for the specified key
     */
    public synchronized boolean containsKey(K key) {
        return storage.containsKey(key);
    }

    /**
     * Returns a Set view of the keys contained in this cache.
     * The set is a snapshot and modifications to it do not affect the cache.
     *
     * @return a set view of the keys contained in this cache
     */
    public synchronized Set<K> keySet() {
        return new HashSet<>(storage.keySet());
    }

    /**
     * Returns a Map view of the entries in this cache.
     * The map is a snapshot and modifications to it do not affect the cache.
     *
     * @return a map containing all entries in this cache
     */
    public synchronized Map<K, V> asMap() {
        return new HashMap<>(storage);
    }

    /**
     * Updates the access order for a key, marking it as most recently used.
     * Removes any existing references to this key (including stale ones) and adds
     * a new reference at the end of the access order list.
     */
    private void updateAccessOrder(K key) {
        // Remove all existing references to this key (handles both stale refs and duplicates)
        accessOrder.removeIf(ref -> {
            K k = ref.get();
            return k == null || key.equals(k);
        });

        // Add to end (most recently used position)
        accessOrder.addLast(new WeakReference<>(key));
    }

    /**
     * Removes a key from the access order list.
     * Also removes any stale references encountered.
     */
    private void removeFromAccessOrder(K key) {
        accessOrder.removeIf(ref -> {
            K k = ref.get();
            return k == null || key.equals(k);
        });
    }

    /**
     * Evicts the least recently used entry that still has a live key.
     * Skips over WeakReferences whose keys have been garbage collected.
     *
     * This performs ACTIVE EVICTION due to size limits, which is different from
     * the automatic removal that happens when keys are GC'd (handled by WeakHashMap).
     */
    private void evictLeastRecentlyUsed() {
        // Iterate from oldest to newest, looking for a live key to evict
        while (!accessOrder.isEmpty()) {
            WeakReference<K> ref = accessOrder.pollFirst();
            K key = ref.get();

            if (key != null) {
                // Found a live key - actively remove it for LRU eviction
                storage.remove(key);
                return;
            }
            // Key was already GC'd, continue to find next candidate
        }

        // Edge case: All references were stale but storage might still have entries
        // (can happen if keys were GC'd between checking size and calling this method)
        // Evict any entry from storage to enforce size limit
        if (!storage.isEmpty()) {
            K anyKey = storage.keySet().iterator().next();
            storage.remove(anyKey);
        }
    }

    /**
     * Removes WeakReference objects whose keys have been garbage collected.
     * Called periodically to prevent the accessOrder list from growing unbounded.
     */
    private void cleanStaleReferences() {
        accessOrder.removeIf(ref -> ref.get() == null);
    }
}