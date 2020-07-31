/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class LazyAtomicReferenceTest {

    @Test
    public void testInitialize() {
        LazyAtomicReference<String> lar = new LazyAtomicReference<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "It worked!";
            }
        });
        
        Assert.assertEquals("It worked!", lar.get());
    }
    
    @Test
    public void testInitializeAndThenSet() {
        LazyAtomicReference<String> lar = new LazyAtomicReference<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "It was initialized!";
            }
        });
        
        Assert.assertEquals("It was initialized!", lar.get());       
        lar.set("It worked!");
        Assert.assertEquals("It worked!", lar.get());
    }
    
    @Test
    public void testSetAndThenInitialize() {
        LazyAtomicReference<String> lar = new LazyAtomicReference<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "It failed!";
            }
        });
        
        lar.set("It worked!");
        Assert.assertEquals("It worked!", lar.get());
    }
    
    @Test(expected=NullPointerException.class)
    public void testNegativeCannotInitializeToNull() {
        LazyAtomicReference<String> lar = new LazyAtomicReference<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return null; // not allowed
            }
        });
        
        Assert.assertFalse("Oops, trouble", lar.get() instanceof Object); 
    }
    
    @Test(expected=RuntimeException.class)
    public void testNegativeExceptionInInitializer() {
        LazyAtomicReference<String> lar = new LazyAtomicReference<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                throw new ExecutionException(new IllegalStateException("you've wired its feet to the perch"));
            }
        });
        
        Assert.assertFalse("Oops, trouble", lar.get() instanceof Object); 
    }
    
    private static AtomicInteger sideEffects = new AtomicInteger(0);

    // Attempt to test the exactly-once initialization feature.
    @Test
    public void testExactlyOnceInitialization() throws Exception {
        final LazyAtomicReference<Integer> target = new LazyAtomicReference<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                // Every call returns a different value.
                return sideEffects.getAndIncrement();
            }
        }
        );
        
        sideEffects.set(0);
        ExecutorService pool = Executors.newFixedThreadPool(10);        
        List<Callable<Integer>> tasks = new ArrayList<>(10);
        for (int i = 0; i < 10; ++i) {
            tasks.add(new Callable<Integer>() {
                @Override
                public Integer call() {
                    // This task should always get the same value.
                    // Cross-task checking occurs below. The value of
                    // 100,000 takes about 0.03 seconds on my Mac,
                    // which should be enough to ensure some actual
                    // overlap of the tasks.
                    int n = target.get();
                    for (int i = 0; i < 100000; ++i) {
                        Assert.assertEquals(n, (int)target.get());
                    }
                    return n;
                }            
            });
        }
        
        // Now check that all 10 tasks got the same value
        List<Future<Integer>> futures = pool.invokeAll(tasks);
        int result = futures.get(0).get();
        for (int i = 1; i < futures.size(); ++i) {
            Assert.assertEquals(result, (int)futures.get(i).get());
        }
        
        pool.shutdownNow();
    }
}
