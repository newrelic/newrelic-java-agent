/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.bridge.CacheRemovalListener;
import com.newrelic.agent.bridge.CleanableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for Caffeine2CollectionFactory, specifically focusing on TimeUnit handling
 * in removal listener methods for Java 8-10.
 *
 * Assisted with AI
 */
public class Caffeine2CollectionFactoryTest {

    private Caffeine2CollectionFactory factory;

    @Before
    public void setUp() {
        factory = new Caffeine2CollectionFactory();
    }

    @Test
    public void testCreateCacheWithWriteExpirationAndRemovalListener_withSeconds() {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                2, TimeUnit.SECONDS, 10, listener);

        assertNotNull("Cache should be created", cache);
        assertTrue("Cache should be empty initially", cache.isEmpty());
    }

    @Test
    public void testCreateCacheWithWriteExpirationAndRemovalListener_withMilliseconds() {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                100, TimeUnit.MILLISECONDS, 10, listener);

        assertNotNull("Cache should be created", cache);
        cache.put("key1", "value1");
        assertEquals("Should have one entry", 1, cache.size());
    }

    @Test
    public void testCreateCacheWithWriteExpirationAndRemovalListener_withNanoseconds() {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                100_000_000, TimeUnit.NANOSECONDS, 10, listener);

        assertNotNull("Cache should be created", cache);
        cache.put("key1", "value1");
        assertEquals("Should have one entry", 1, cache.size());
    }

    @Test
    public void testCreateCacheWithAccessExpirationAndRemovalListener_withSeconds() {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        CleanableMap<String, String> cache = factory.createCacheWithAccessExpirationAndRemovalListener(
                2, TimeUnit.SECONDS, 10, listener);

        assertNotNull("Cache should be created", cache);
        assertTrue("Cache should be empty initially", cache.isEmpty());
    }

    @Test
    public void testCreateCacheWithAccessExpirationAndRemovalListener_withMilliseconds() {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        CleanableMap<String, String> cache = factory.createCacheWithAccessExpirationAndRemovalListener(
                100, TimeUnit.MILLISECONDS, 10, listener);

        assertNotNull("Cache should be created", cache);
        cache.put("key1", "value1");
        assertEquals("Should have one entry", 1, cache.size());
    }

    @Test
    public void testWriteExpirationActuallyExpires() throws InterruptedException {
        List<String> removedKeys = new ArrayList<>();
        List<CacheRemovalListener.RemovalReason> removalReasons = new ArrayList<>();

        CacheRemovalListener<String, String> listener = (key, value, reason) -> {
            removedKeys.add(key);
            removalReasons.add(reason);
        };

        // Create cache with 100ms write expiration
        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                100, TimeUnit.MILLISECONDS, 10, listener);

        cache.put("key1", "value1");
        assertEquals("Should have one entry", 1, cache.size());

        // Wait for expiration
        Thread.sleep(150);

        // Trigger cleanup
        cache.cleanUp();

        // Verify entry was removed
        assertEquals("Entry should have been removed", 0, cache.size());
        assertEquals("Listener should have been called once", 1, removedKeys.size());
        assertEquals("Removed key should be key1", "key1", removedKeys.get(0));
        assertEquals("Removal reason should be EXPIRED", CacheRemovalListener.RemovalReason.EXPIRED, removalReasons.get(0));
    }

    @Test
    public void testAccessExpirationActuallyExpires() throws InterruptedException {
        List<String> removedKeys = new ArrayList<>();
        List<CacheRemovalListener.RemovalReason> removalReasons = new ArrayList<>();

        CacheRemovalListener<String, String> listener = (key, value, reason) -> {
            removedKeys.add(key);
            removalReasons.add(reason);
        };

        // Create cache with 100ms access expiration
        CleanableMap<String, String> cache = factory.createCacheWithAccessExpirationAndRemovalListener(
                100, TimeUnit.MILLISECONDS, 10, listener);

        cache.put("key1", "value1");
        assertEquals("Should have one entry", 1, cache.size());

        // Wait for expiration without accessing
        Thread.sleep(150);

        // Trigger cleanup
        cache.cleanUp();

        // Verify entry was removed
        assertEquals("Entry should have been removed", 0, cache.size());
        assertEquals("Listener should have been called once", 1, removedKeys.size());
        assertEquals("Removed key should be key1", "key1", removedKeys.get(0));
        assertEquals("Removal reason should be EXPIRED", CacheRemovalListener.RemovalReason.EXPIRED, removalReasons.get(0));
    }

    @Test
    public void testAccessExpirationResetsOnAccess() throws InterruptedException {
        AtomicInteger removalCount = new AtomicInteger(0);
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removalCount.incrementAndGet();

        // Create cache with 150ms access expiration
        CleanableMap<String, String> cache = factory.createCacheWithAccessExpirationAndRemovalListener(
                150, TimeUnit.MILLISECONDS, 10, listener);

        cache.put("key1", "value1");

        // Access the entry every 75ms (before expiration)
        for (int i = 0; i < 3; i++) {
            Thread.sleep(75);
            cache.get("key1"); // Reset access time
        }

        // Entry should still be present after 225ms total
        cache.cleanUp();
        assertEquals("Entry should still be present", 1, cache.size());
        assertEquals("No removals should have occurred", 0, removalCount.get());
    }

    @Test
    public void testRemovalListenerCalledOnExplicitRemoval() {
        List<CacheRemovalListener.RemovalReason> removalReasons = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removalReasons.add(reason);

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                10, TimeUnit.SECONDS, 10, listener);

        cache.put("key1", "value1");
        cache.remove("key1");

        assertEquals("Listener should have been called", 1, removalReasons.size());
        assertEquals("Removal reason should be EXPLICIT", CacheRemovalListener.RemovalReason.EXPLICIT, removalReasons.get(0));
    }

    @Test
    public void testRemovalListenerCalledOnReplace() {
        List<CacheRemovalListener.RemovalReason> removalReasons = new ArrayList<>();
        List<String> removedValues = new ArrayList<>();

        CacheRemovalListener<String, String> listener = (key, value, reason) -> {
            removalReasons.add(reason);
            removedValues.add(value);
        };

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                10, TimeUnit.SECONDS, 10, listener);

        cache.put("key1", "value1");
        cache.put("key1", "value2"); // Replace

        assertEquals("Listener should have been called once", 1, removalReasons.size());
        assertEquals("Removal reason should be REPLACED", CacheRemovalListener.RemovalReason.REPLACED, removalReasons.get(0));
        assertEquals("Removed value should be value1", "value1", removedValues.get(0));
    }

    @Test
    public void testMultipleEntriesWithWriteExpiration() throws InterruptedException {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                100, TimeUnit.MILLISECONDS, 10, listener);

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        assertEquals("Should have three entries", 3, cache.size());

        Thread.sleep(150);
        cache.cleanUp();

        assertEquals("All entries should be removed", 0, cache.size());
        assertEquals("Listener should be called three times", 3, removedKeys.size());
        assertTrue("key1 should be removed", removedKeys.contains("key1"));
        assertTrue("key2 should be removed", removedKeys.contains("key2"));
        assertTrue("key3 should be removed", removedKeys.contains("key3"));
    }

    @Test
    public void testWriteExpirationWithDifferentTimeUnits() throws InterruptedException {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                100, TimeUnit.MILLISECONDS, 10, listener);

        cache.put("key1", "value1");
        Thread.sleep(150);
        cache.cleanUp();

        assertEquals("Entry should be removed", 0, cache.size());
        assertEquals("Listener should be called", 1, removedKeys.size());
    }

    @Test
    public void testAccessExpirationWithDifferentTimeUnits() throws InterruptedException {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        // Test with MICROSECONDS
        CleanableMap<String, String> cache = factory.createCacheWithAccessExpirationAndRemovalListener(
                100_000, TimeUnit.MICROSECONDS, 10, listener);

        cache.put("key1", "value1");
        Thread.sleep(150); // 150ms = 150,000 microseconds
        cache.cleanUp();

        assertEquals("Entry should be removed", 0, cache.size());
        assertEquals("Listener should be called", 1, removedKeys.size());
    }

    @Test
    public void testCleanUpMethodWorks() throws InterruptedException {
        List<String> removedKeys = new ArrayList<>();
        CacheRemovalListener<String, String> listener = (key, value, reason) -> removedKeys.add(key);

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                50, TimeUnit.MILLISECONDS, 10, listener);

        cache.put("key1", "value1");
        Thread.sleep(100);

        // Before cleanup, entry may still appear in size due to lazy eviction
        cache.cleanUp();

        // After cleanup, expired entries should be removed
        assertEquals("Entry should be removed after cleanup", 0, cache.size());
        assertEquals("Listener should be called", 1, removedKeys.size());
    }

    @Test
    public void testInitialCapacityRespected() {
        CacheRemovalListener<String, String> listener = (key, value, reason) -> {};

        CleanableMap<String, String> cache = factory.createCacheWithWriteExpirationAndRemovalListener(
                10, TimeUnit.SECONDS, 100, listener);

        // Add entries up to initial capacity - should work without issues
        for (int i = 0; i < 100; i++) {
            cache.put("key" + i, "value" + i);
        }

        assertEquals("Should have 100 entries", 100, cache.size());
    }
}