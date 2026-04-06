/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.extension;

import com.newrelic.agent.bridge.ExtensionHolder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Tests for ExtensionHolderFactoryImpl focusing on thread contention scenarios.
 * 
 * These tests verify that the putIfAbsent pattern properly handles high concurrency
 * without causing thread contention issues that were observed with computeIfAbsent.
 */
public class ExtensionHolderFactoryImplTest {

    private ExtensionHolderFactoryImpl factory;

    @Before
    public void setUp() {
        factory = new ExtensionHolderFactoryImpl();
    }

    @Test
    public void testBasicGetExtension() {
        ExtensionHolder<TestExtension> holder = factory.build();
        Object instance = new Object();
        
        TestExtension extension = holder.getExtension(instance, TestExtension::new);
        
        assertNotNull(extension);
        // Same instance should be returned on subsequent calls
        assertSame(extension, holder.getExtension(instance, TestExtension::new));
    }

    @Test
    public void testGetAndRemoveExtension() {
        ExtensionHolder<TestExtension> holder = factory.build();
        Object instance = new Object();
        
        TestExtension extension = holder.getExtension(instance, TestExtension::new);
        TestExtension removed = holder.getAndRemoveExtension(instance);
        
        assertSame(extension, removed);
        // After removal, a new instance should be created
        TestExtension newExtension = holder.getExtension(instance, TestExtension::new);
        assertNotSame(extension, newExtension);
    }

    @Test
    public void testDifferentInstancesGetDifferentExtensions() {
        ExtensionHolder<TestExtension> holder = factory.build();
        Object instance1 = new Object();
        Object instance2 = new Object();
        
        TestExtension extension1 = holder.getExtension(instance1, TestExtension::new);
        TestExtension extension2 = holder.getExtension(instance2, TestExtension::new);
        
        assertNotSame(extension1, extension2);
    }

    /**
     * Test high concurrency access to the same instance key.
     * This verifies that the putIfAbsent pattern correctly handles race conditions
     * where multiple threads try to create an extension for the same instance.
     */
    @Test
    public void testHighConcurrencySameInstance() throws Exception {
        ExtensionHolder<TestExtension> holder = factory.build();
        Object sharedInstance = new Object();
        int threadCount = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<TestExtension> firstExtension = new AtomicReference<>();
        AtomicInteger successCount = new AtomicInteger(0);
        
        List<Future<TestExtension>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    TestExtension ext = holder.getExtension(sharedInstance, TestExtension::new);
                    firstExtension.compareAndSet(null, ext);
                    successCount.incrementAndGet();
                    return ext;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }
        
        // Release all threads simultaneously to maximize contention
        startLatch.countDown();
        
        // Wait for completion with timeout
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertTrue("Test timed out - possible thread contention deadlock", completed);
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // All threads should have succeeded
        assertEquals(threadCount, successCount.get());
        
        // All threads should have received the same extension instance
        TestExtension expected = firstExtension.get();
        assertNotNull(expected);
        for (Future<TestExtension> future : futures) {
            assertSame("All threads should receive the same extension instance", 
                    expected, future.get());
        }
    }

    /**
     * Test high concurrency access with different instance keys.
     * This simulates real-world usage where many different objects need extensions.
     */
    @Test
    public void testHighConcurrencyDifferentInstances() throws Exception {
        ExtensionHolder<TestExtension> holder = factory.build();
        int threadCount = 100;
        int instancesPerThread = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < instancesPerThread; j++) {
                        Object instance = new Object();
                        TestExtension ext = holder.getExtension(instance, TestExtension::new);
                        assertNotNull(ext);
                    }
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        assertTrue("Test timed out - possible thread contention deadlock", completed);
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        assertEquals(threadCount, successCount.get());
    }

    /**
     * Test that simulates the contention scenario that caused issues with computeIfAbsent.
     * Uses a slow value loader to increase the window for contention.
     */
    @Test
    public void testContentionWithSlowValueLoader() throws Exception {
        ExtensionHolder<TestExtension> holder = factory.build();
        Object sharedInstance = new Object();
        int threadCount = 50;
        AtomicInteger creationCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TestExtension ext = holder.getExtension(sharedInstance, () -> {
                        creationCount.incrementAndGet();
                        // Simulate slow creation to widen contention window
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return new TestExtension();
                    });
                    assertNotNull(ext);
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertTrue("Test timed out - possible thread contention deadlock", completed);
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        assertEquals(threadCount, successCount.get());
        
        // With putIfAbsent pattern, multiple creations may occur but all threads succeed
        // This is the expected trade-off vs computeIfAbsent which could cause contention
        System.out.println("Extension creations: " + creationCount.get() + 
                " (may be > 1 due to race, but all threads completed successfully)");
    }

    /**
     * Simple test extension class for testing purposes.
     */
    private static class TestExtension {
        // Empty extension class - mirrors real usage where extensions are stateless
    }
}
