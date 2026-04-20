package com.newrelic.weave.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Tests weak key behavior, LRU eviction, size limiting, and thread safety.
 * Assisted by AI
 */
public class WeakKeyLruCacheTest {
    @Test
    public void testPutAndGet() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testPutReplacesOldValue() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        String oldValue = cache.put("key1", "value1");
        assertNull("Should return null for new key", oldValue);

        oldValue = cache.put("key1", "value2");
        assertEquals("Should return old value", "value1", oldValue);
        assertEquals("value2", cache.get("key1"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testRemove() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        assertEquals("value1", cache.remove("key1"));
        assertNull(cache.get("key1"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testClear() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        assertEquals(3, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
    }

    @Test
    public void testContainsKey() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        assertFalse(cache.containsKey("key1"));
        cache.put("key1", "value1");
        assertTrue(cache.containsKey("key1"));
    }

    @Test
    public void testPutIfAbsent() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        String result = cache.putIfAbsent("key1", "value1");
        assertNull("Should return null for new key", result);
        assertEquals("value1", cache.get("key1"));

        result = cache.putIfAbsent("key1", "value2");
        assertEquals("Should return existing value", "value1", result);
        assertEquals("Original value should remain", "value1", cache.get("key1"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullKeyThrowsException() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);
        cache.put(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxSize() {
        new WeakKeyLruCache<String, String>(0);
    }

    @Test
    public void testLruEviction() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(3);

        // Fill cache to capacity
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        assertEquals(3, cache.size());

        // Add fourth entry - should evict key1 (least recently used)
        cache.put("key4", "value4");

        assertEquals(3, cache.size());
        assertNull("key1 should have been evicted", cache.get("key1"));
        assertNotNull("key2 should still exist", cache.get("key2"));
        assertNotNull("key3 should still exist", cache.get("key3"));
        assertNotNull("key4 should exist", cache.get("key4"));
    }

    @Test
    public void testLruEvictionWithAccess() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(3);

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        // Access key1 to make it most recently used
        cache.get("key1");

        // Add key4 - should evict key2 (now least recently used)
        cache.put("key4", "value4");

        assertEquals(3, cache.size());
        assertNotNull("key1 should still exist (was accessed)", cache.get("key1"));
        assertNull("key2 should have been evicted", cache.get("key2"));
        assertNotNull("key3 should still exist", cache.get("key3"));
        assertNotNull("key4 should exist", cache.get("key4"));
    }

    @Test
    public void testLruEvictionOrder() {
        WeakKeyLruCache<Integer, String> cache = new WeakKeyLruCache<>(3);

        // Add entries in order: 1, 2, 3
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Add 4 - should evict 1
        cache.put(4, "four");
        assertNull(cache.get(1));

        // Add 5 - should evict 2
        cache.put(5, "five");
        assertNull(cache.get(2));

        // Remaining should be 3, 4, 5
        assertNotNull(cache.get(3));
        assertNotNull(cache.get(4));
        assertNotNull(cache.get(5));
    }

    @Test
    public void testUpdateExistingKeyDoesNotEvict() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(3);

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        // Update existing key - should not evict anything
        cache.put("key2", "value2-updated");

        assertEquals(3, cache.size());
        assertNotNull(cache.get("key1"));
        assertEquals("value2-updated", cache.get("key2"));
        assertNotNull(cache.get("key3"));
    }

    @Test
    public void testSizeOneCache() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(1);

        cache.put("key1", "value1");
        assertEquals(1, cache.size());

        cache.put("key2", "value2");
        assertEquals(1, cache.size());
        assertNull("key1 should be evicted", cache.get("key1"));
        assertNotNull("key2 should exist", cache.get("key2"));
    }

    @Test
    public void testWeakKeysAllowGarbageCollection() throws InterruptedException {
        WeakKeyLruCache<Object, String> cache = new WeakKeyLruCache<>(10);

        // Create keys that can be GC'd
        Object key1 = new Object();
        Object key2 = new Object();
        Object key3 = new Object();

        cache.put(key1, "value1");
        cache.put(key2, "value2");
        cache.put(key3, "value3");
        assertEquals(3, cache.size());

        // Null out references to allow GC
        key1 = null;
        key2 = null;

        // Force garbage collection
        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(50);
        }

        // WeakHashMap should have cleaned up the GC'd keys
        int sizeAfterGC = cache.size();
        assertTrue("Cache size should decrease after GC", sizeAfterGC < 3);
        assertTrue("Cache should have at most 1 entry left", sizeAfterGC <= 1);
    }

    @Test
    public void testWeakKeysWithClassLoaders() throws InterruptedException {
        WeakKeyLruCache<ClassLoader, String> cache = new WeakKeyLruCache<>(10);

        // Create ClassLoaders that can be GC'd
        ClassLoader cl1 = new ClassLoader() {};
        ClassLoader cl2 = new ClassLoader() {};
        ClassLoader cl3 = new ClassLoader() {};

        cache.put(cl1, "value1");
        cache.put(cl2, "value2");
        cache.put(cl3, "value3");
        assertEquals(3, cache.size());

        // Null out references
        cl1 = null;
        cl2 = null;

        // Force GC
        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(50);
        }

        int sizeAfterGC = cache.size();
        assertTrue("ClassLoaders should be GC'd", sizeAfterGC <= 1);
    }

    @Test
    public void testStaleReferenceCleanup() throws InterruptedException {
        WeakKeyLruCache<Object, String> cache = new WeakKeyLruCache<>(10);

        // Add and GC many keys to create stale references
        for (int i = 0; i < 50; i++) {
            Object key = new Object();
            cache.put(key, "value" + i);
            // key goes out of scope immediately
        }

        // Force GC to clean up keys
        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(50);
        }

        // Add more entries to trigger cleanup (when accessOrder.size() > maxSize * 2)
        for (int i = 0; i < 25; i++) {
            cache.put("live-key-" + i, "value" + i);
        }

        // The accessOrder list should have been cleaned up
        // Hard to test directly, but we can verify the cache still works correctly
        assertTrue("Cache should respect max size", cache.size() <= 10);
    }

    @Test
    public void testConcurrentPutsAndGets() throws InterruptedException, ExecutionException {
        final WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(100);
        final int numThreads = 10;
        final int operationsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        // Launch threads that concurrently put and get
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    String key = "thread" + threadId + "-key" + i;
                    cache.put(key, "value" + i);
                    cache.get(key);
                }
            }));
        }

        // Wait for all threads to complete
        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Verify cache respects max size
        assertTrue("Cache should not exceed max size", cache.size() <= 100);
    }

    @Test
    public void testConcurrentPutIfAbsent() throws InterruptedException, ExecutionException {
        final WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(50);
        final int numThreads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<String>> futures = new ArrayList<>();

        // Multiple threads try to putIfAbsent the same key
        final String sharedKey = "shared-key";
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            futures.add(executor.submit(() ->
                cache.putIfAbsent(sharedKey, "value-from-thread-" + threadId)
            ));
        }

        // Wait for all threads
        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Exactly one thread should have succeeded (returned null)
        long nullCount = results.stream().filter(r -> r == null).count();
        assertEquals("Exactly one thread should succeed with putIfAbsent", 1, nullCount);

        // All other threads should have gotten the same value
        String winningValue = cache.get(sharedKey);
        assertNotNull(winningValue);
        for (String result : results) {
            if (result != null) {
                assertEquals(winningValue, result);
            }
        }
    }

    @Test
    public void testConcurrentEviction() throws InterruptedException, ExecutionException {
        final WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(50);
        final int numThreads = 20;
        final int operationsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        // Hammer the cache with concurrent operations that will trigger evictions
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    String key = "thread" + threadId + "-iter" + i;
                    cache.put(key, "value");
                    cache.get(key);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Cache should still respect max size despite concurrent evictions
        assertTrue("Cache should not exceed max size", cache.size() <= 50);
    }

    // ============================================================================
    // Weak Key + LRU Interaction Tests
    // ============================================================================

    @Test
    public void testEvictionWithSomeGCdKeys() throws InterruptedException {
        WeakKeyLruCache<Object, String> cache = new WeakKeyLruCache<>(3);

        Object key1 = new Object();
        Object key2 = new Object();
        Object key3 = new Object();

        cache.put(key1, "value1");
        cache.put(key2, "value2");
        cache.put(key3, "value3");
        assertEquals(3, cache.size());

        // GC key1
        key1 = null;
        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(50);
        }

        // Now cache has 2 live entries (key2, key3)
        int sizeAfterGC = cache.size();
        assertTrue("Size should be less than 3", sizeAfterGC < 3);

        // Add new entries - should fill up to 3 without evicting
        cache.put(new Object(), "value4");
        assertTrue("Should have room for new entry", cache.size() <= 3);
    }

    @Test
    public void testEvictionPrefersGCdKeysOverLru() throws InterruptedException {
        WeakKeyLruCache<Object, String> cache = new WeakKeyLruCache<>(3);

        Object key1 = new Object();
        Object key2 = new Object();  // Will hold reference to this one
        Object key3 = new Object();

        cache.put(key1, "value1");
        cache.put(key2, "value2");
        cache.put(key3, "value3");

        // GC key1 and key3
        key1 = null;
        key3 = null;
        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(50);
        }

        // Cache should now have only key2
        // Add two more entries
        Object key4 = new Object();
        Object key5 = new Object();
        cache.put(key4, "value4");
        cache.put(key5, "value5");

        // key2 should still be present (wasn't evicted)
        assertNotNull("key2 should still exist", cache.get(key2));
        assertEquals(3, cache.size());
    }

    @Test
    public void testClassLoaderCachingPattern() {
        // Simulates how WeavePackageManager uses the cache
        WeakKeyLruCache<ClassLoader, ConcurrentMap<String, Object>> cache = new WeakKeyLruCache<>(100);

        ClassLoader cl1 = new ClassLoader() {};
        ClassLoader cl2 = new ClassLoader() {};

        // Pattern: putIfAbsent to get or create the inner map
        ConcurrentMap<String, Object> map1 = cache.putIfAbsent(cl1, new ConcurrentHashMap<>());
        assertNull("First putIfAbsent should return null for new key", map1);
        map1 = cache.get(cl1);
        assertNotNull("Cache should contain the newly added entry", map1);
        map1.put("package1", new Object());

        ConcurrentMap<String, Object> map2 = cache.putIfAbsent(cl2, new ConcurrentHashMap<>());
        assertNull("Second putIfAbsent should return null for new key", map2);
        map2 = cache.get(cl2);
        assertNotNull("Cache should contain the newly added entry (map2)", map2);
        map2.put("package2", new Object());

        assertEquals(2, cache.size());
        assertNotNull(cache.get(cl1));
        assertEquals(1, cache.get(cl1).size());
    }

    @Test
    public void testManyClassLoadersExceedingLimit() {
        WeakKeyLruCache<ClassLoader, String> cache = new WeakKeyLruCache<>(100);

        // Add 200 ClassLoaders (2x the limit)
        List<ClassLoader> firstHundred = new ArrayList<>();
        List<ClassLoader> secondHundred = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            ClassLoader cl = new ClassLoader() {};
            if (i < 100) {
                firstHundred.add(cl);
            } else {
                secondHundred.add(cl);
            }
            cache.put(cl, "value" + i);
        }

        // Cache should be at max size
        assertTrue("Cache should not exceed max size", cache.size() <= 100);

        // With LRU eviction, the LAST 100 (most recent) should be in cache
        int foundInSecondHundred = 0;
        for (ClassLoader cl : secondHundred) {
            if (cache.get(cl) != null) {
                foundInSecondHundred++;
            }
        }
        assertTrue("Most of the last 100 should be in cache due to LRU", foundInSecondHundred >= 90);

        // First 100 (oldest) should have been evicted
        int foundInFirstHundred = 0;
        for (ClassLoader cl : firstHundred) {
            if (cache.get(cl) != null) {
                foundInFirstHundred++;
            }
        }
        assertTrue("First 100 should be mostly evicted", foundInFirstHundred < 10);
    }

    @Test
    public void testGetNonExistentKey() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);
        assertNull(cache.get("nonexistent"));
    }

    @Test
    public void testGetNullKey() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);
        assertNull("Getting null key should return null", cache.get(null));
    }

    @Test
    public void testRemoveNullKey() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);
        assertNull("Removing null key should return null", cache.remove(null));
    }

    @Test
    public void testEmptyCache() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("anything"));
        assertTrue(cache.keySet().isEmpty());
        assertTrue(cache.asMap().isEmpty());
    }

    @Test
    public void testAsMapReturnsSnapshot() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        Map<String, String> snapshot = cache.asMap();
        assertEquals(2, snapshot.size());

        // Modifying snapshot shouldn't affect cache
        snapshot.put("key3", "value3");
        assertEquals(2, cache.size());
        assertNull(cache.get("key3"));
    }

    @Test
    public void testKeySetReturnsSnapshot() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        Set<String> keys = cache.keySet();
        assertEquals(2, keys.size());

        // Modifying keyset shouldn't affect cache
        keys.add("key3");
        assertEquals(2, cache.size());
    }

    @Test
    public void testLargeNumberOfOperations() {
        WeakKeyLruCache<Integer, String> cache = new WeakKeyLruCache<>(100);

        // Perform many operations
        for (int i = 0; i < 1000; i++) {
            cache.put(i, "value" + i);
            if (i % 3 == 0) {
                cache.get(i / 2);  // Access some entries
            }
        }

        // Should have evicted down to max size
        assertTrue("Cache should not exceed max size", cache.size() <= 100);
    }

    @Test(timeout = 5000)
    public void testPerformanceIsReasonable() {
        WeakKeyLruCache<Integer, String> cache = new WeakKeyLruCache<>(100);

        // This should complete quickly even with linear scans
        for (int i = 0; i < 10000; i++) {
            cache.put(i, "value" + i);
            cache.get(i);
        }

        assertEquals(100, cache.size());
    }

    @Test
    public void testPutIfAbsentWithExistingKey() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        String result = cache.putIfAbsent("key1", "value2");

        assertEquals("Should return existing value", "value1", result);
        assertEquals("Value should not be replaced", "value1", cache.get("key1"));
    }

    @Test
    public void testKeySet() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        Set<String> keys = cache.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));

        // Verify it's a snapshot - modifications don't affect cache
        keys.clear();
        assertEquals(3, cache.size());
    }

    @Test
    public void testAsMap() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        Map<String, String> map = cache.asMap();
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));

        // Verify it's a snapshot - modifications don't affect cache
        map.clear();
        assertEquals(2, cache.size());
    }

    @Test
    public void testMaxSizeOne() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(1);

        cache.put("key1", "value1");
        assertEquals(1, cache.size());

        cache.put("key2", "value2");
        assertEquals(1, cache.size());
        assertNull("First key should be evicted", cache.get("key1"));
        assertNotNull("Second key should be present", cache.get("key2"));
    }

    @Test
    public void testBehaviorAfterClear() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());

        // Verify cache works normally after clear
        cache.put("key3", "value3");
        assertEquals(1, cache.size());
        assertEquals("value3", cache.get("key3"));
    }

    @Test
    public void testStaleReferenceCleanupTrigger() {
        WeakKeyLruCache<Object, String> cache = new WeakKeyLruCache<>(10);

        // Add enough entries to trigger cleanup (> maxSize * 2)
        for (int i = 0; i < 25; i++) {
            Object key = new Object();
            cache.put(key, "value" + i);
            // Keys immediately become eligible for GC as they go out of scope
        }

        // Cache should maintain size limit despite stale references
        assertTrue("Cache should respect max size", cache.size() <= 10);
    }

    @Test
    public void testGetNonExistentKeyDoesNotAffectAccessOrder() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(2);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        // Get non-existent key shouldn't affect eviction order
        cache.get("nonexistent");

        cache.put("key3", "value3");

        // key1 should be evicted (oldest), not affected by failed get
        assertNull("key1 should be evicted", cache.get("key1"));
        assertNotNull("key2 should remain", cache.get("key2"));
        assertNotNull("key3 should be present", cache.get("key3"));
    }

    @Test
    public void testConcurrentPutsWithSameKey() throws InterruptedException {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(100);
        int threadCount = 10;
        int iterationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Multiple threads putting to the same key
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    cache.put("sharedKey", "thread" + threadId + "-" + j);
                }
                latch.countDown();
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);

        // Should have exactly one entry
        assertEquals(1, cache.size());
        assertNotNull(cache.get("sharedKey"));
    }

    @Test
    public void testAccessOrderUpdatesOnGet() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(2);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        // Access key1 to make it most recently used
        cache.get("key1");

        // Add key3, should evict key2 (now least recently used)
        cache.put("key3", "value3");

        assertNotNull("key1 should remain (was accessed)", cache.get("key1"));
        assertNull("key2 should be evicted (least recently used)", cache.get("key2"));
        assertNotNull("key3 should be present", cache.get("key3"));
    }

    @Test
    public void testMultipleUpdatesToSameKeyMaintainsSize() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        // Update the same key multiple times
        for (int i = 0; i < 100; i++) {
            cache.put("key1", "value" + i);
        }

        assertEquals(1, cache.size());
        assertEquals("value99", cache.get("key1"));
    }

    @Test
    public void testNullValueHandling() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(3);

        // Store a null value
        cache.put("key1", null);
        assertEquals(1, cache.size());
        assertTrue("Cache should contain key with null value", cache.containsKey("key1"));
        assertNull("Should return null value", cache.get("key1"));

        // Add more entries
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        // Access the null value entry to update its LRU position
        assertNull("Should still return null value", cache.get("key1"));

        // Add a 4th entry, should evict key2 (least recently used), not key1
        cache.put("key4", "value4");

        assertTrue("key1 with null value should still be present", cache.containsKey("key1"));
        assertNull("key2 should be evicted", cache.get("key2"));
        assertNotNull("key3 should be present", cache.get("key3"));
        assertNotNull("key4 should be present", cache.get("key4"));
    }

    @Test
    public void testPutIfAbsentWithNullValue() {
        WeakKeyLruCache<String, String> cache = new WeakKeyLruCache<>(10);

        // Store a null value
        cache.put("key1", null);

        // putIfAbsent should NOT overwrite the null value
        String result = cache.putIfAbsent("key1", "newValue");
        assertNull("Should return the existing null value", result);
        assertNull("Value should still be null (not overwritten)", cache.get("key1"));

        // putIfAbsent on non-existent key should work normally
        result = cache.putIfAbsent("key2", "value2");
        assertNull("Should return null for new key", result);
        assertEquals("value2", cache.get("key2"));
    }
}