package com.newrelic.api.agent;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConcurrentHashMapHeadersTest {

    private ConcurrentHashMapHeaders target;

    @Before
    public void setup() {
        target = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
    }

    @Test
    public void getHeaderType() {
        assertEquals(HeaderType.HTTP, ConcurrentHashMapHeaders.build(HeaderType.HTTP).getHeaderType());
        assertEquals(HeaderType.MESSAGE, ConcurrentHashMapHeaders.build(HeaderType.MESSAGE).getHeaderType());
    }

    @Test
    public void addSetRemoveContainsHeader() {
        String h1Key = "h1Key";
        String h1Value1 = "h1Value1";
        String h1Value2 = "h1Value2";

        // Assert behavior when empty
        assertNull(target.getHeader(h1Key));
        assertEquals(Collections.emptyList(), target.getHeaders(h1Key));
        assertEquals(Collections.emptySet(), target.getHeaderNames());
        assertFalse(target.containsHeader(h1Key));

        // Set a header
        target.setHeader(h1Key, h1Value1);
        assertEquals(h1Value1, target.getHeader(h1Key));
        assertEquals(Collections.singletonList(h1Value1), target.getHeaders(h1Key));
        assertEquals(Collections.singleton(h1Key), target.getHeaderNames());
        assertTrue(target.containsHeader(h1Key));

        // Set a new header value
        target.setHeader(h1Key, h1Value2);
        assertEquals(h1Value2, target.getHeader(h1Key));
        assertEquals(Collections.singleton(h1Key), target.getHeaderNames());
        assertTrue(target.containsHeader(h1Key));

        // Remove a header
        target.removeHeader(h1Key);
        assertNull(target.getHeader(h1Key));
        assertEquals(Collections.emptySet(), target.getHeaderNames());
        assertFalse(target.containsHeader(h1Key));

        // Add multiple values
        target.addHeader(h1Key, h1Value1);
        target.addHeader(h1Key, h1Value2);
        assertEquals(Arrays.asList(h1Value1, h1Value2), target.getHeaders(h1Key));
        assertEquals(Collections.singleton(h1Key), target.getHeaderNames());
        assertTrue(target.containsHeader(h1Key));

        // Add multiple headers
        String h2Key = "h2Key";
        String h2Value1 = "h2Value1";
        target.addHeader(h2Key, h2Value1);
        assertEquals(h2Value1, target.getHeader(h2Key));
        assertEquals(new HashSet<>(Arrays.asList(h1Key, h2Key)), target.getHeaderNames());
    }

    @Test
    public void concurrentReadAndWrite() throws ExecutionException, InterruptedException {
        // Generate a list of keys to operate against
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            keys.add("key" + i);
        }

        // Build a list of operations to execute concurrently
        final Random random = new Random();
        List<Runnable> operations = new ArrayList<>();
        // Add write operations - a mix of addHeader, setHeader, and removeHeader
        for (int i = 0; i < 5000; i++) {
            final String key = keys.get(random.nextInt(keys.size()));
            final String value = key + i;
            final int operationIndex = random.nextInt(3);
            operations.add(new Runnable() {
                @Override
                public void run() {
                    if (operationIndex == 0) {
                        target.addHeader(key, value);
                    } else if (operationIndex == 1) {
                        target.setHeader(key, value);
                    } else {
                        target.removeHeader(key);
                    }
                }
            });
        }
        // Add read operations - a mix of getHeader, getHeaders, containsHeader, and getHeaderNames, and getReadOnlyMap
        for (int i = 0; i < 5000; i++) {
            final String key = keys.get(random.nextInt(keys.size()));
            final int operationIndex = random.nextInt(5);
            operations.add(new Runnable() {
                @Override
                public void run() {
                    if (operationIndex == 0) {
                        target.getHeader(key);
                    } else if (operationIndex == 1) {
                        target.getHeaders(key);
                    } else if (operationIndex == 2) {
                        target.containsHeader(key);
                    } else if (operationIndex == 3) {
                        target.getHeaderNames();
                    } else {
                        target.getMapCopy();
                    }
                }
            });
        }

        // Shuffle all the operations, add them to the executor, await completion and confirm no exceptions occurred
        Collections.shuffle(operations);
        List<Future<?>> futures = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (Runnable runnable : operations) {
            futures.add(executorService.submit(runnable));
        }
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        for (Future<?> future : futures) {
            future.get();
        }
        executorService.shutdown();
    }

    @Test
    public void buildFromMap() {
        String h1Key = "h1Key";
        String h2Key = "h2Key";
        String h1Value1 = "h1Value1";
        String h1Value2 = "h1Value2";
        String h2Value1 = "h2Value1";

        Map<String, List<String>> map = new HashMap<>();
        map.put(h1Key, Arrays.asList(h1Value1, h1Value2));
        map.put(h2Key, Collections.singletonList(h2Value1));

        target = ConcurrentHashMapHeaders.buildFromMap(HeaderType.HTTP, map);

        assertEquals(new HashSet<>(Arrays.asList(h1Key, h2Key)), target.getHeaderNames());
        assertEquals(Arrays.asList(h1Value1, h1Value2), target.getHeaders(h1Key));
        assertEquals(Collections.singletonList(h2Value1), target.getHeaders(h2Key));
    }

    @Test
    public void buildFromFlatMap() {
        String h1Key = "h1Key";
        String h2Key = "h2Key";
        String h1Value1 = "h1Value1";
        String h2Value1 = "h2Value1";

        Map<String, String> map = new HashMap<>();
        map.put(h1Key, h1Value1);
        map.put(h2Key, h2Value1);

        target = ConcurrentHashMapHeaders.buildFromFlatMap(HeaderType.HTTP, map);

        assertEquals(new HashSet<>(Arrays.asList(h1Key, h2Key)), target.getHeaderNames());
        assertEquals(Collections.singletonList(h1Value1), target.getHeaders(h1Key));
        assertEquals(Collections.singletonList(h2Value1), target.getHeaders(h2Key));
    }

}
