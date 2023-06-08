/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class InsertOnlyArrayTest {

    @Test
    public void testConcurrency() throws InterruptedException {
        final Map<Integer, Integer> map = new ConcurrentHashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        final InsertOnlyArray<Integer> array = new InsertOnlyArray<>(100);

        int count = 100000;
        final AtomicInteger incrementingId = new AtomicInteger();
        try {
            // concurrently add to the array
            for (int i = 0; i < count; i++) {
                executorService.execute(new Runnable() {

                    @Override
                    public void run() {
                        int id = incrementingId.incrementAndGet();
                        int index = array.add(id);
                        map.put(index, id);
                    }
                });

            }
            // throw in some concurrent get calls
            for (int i = 0; i < 1000; i++) {
                executorService.execute(new Runnable() {

                    @Override
                    public void run() {
                        for (Entry<Integer, Integer> entry : map.entrySet()) {
                            Assert.assertEquals(entry.getValue(), array.get(entry.getKey()));
                        }
                    }
                });
            }
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(20, TimeUnit.SECONDS);
        }

        Assert.assertEquals(count, map.size());
        for (Entry<Integer, Integer> entry : map.entrySet()) {
            Assert.assertEquals(entry.getValue(), array.get(entry.getKey()));
        }
    }

    @Test
    public void getIndex_whenElementDoesNotExist_returnsNegative() {
        InsertOnlyArray<Integer> array = new InsertOnlyArray<>(10);
        array.add(1);
        array.add(2);
        array.add(3);

        assertEquals(-1, array.getIndex(100));
    }

    @Test
    public void getIndex_whenElementExists_returnsIndex() {
        InsertOnlyArray<Integer> array = new InsertOnlyArray<>(10);
        array.add(1);
        array.add(2);
        array.add(3);

        assertEquals(2, array.getIndex(3));
    }
}
