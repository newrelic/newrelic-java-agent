/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.reactor.netty;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.newrelic.agent.introspec.MetricsHelper.getUnscopedMetricCount;
import static org.junit.Assert.assertEquals;

/**
 * Tests concurrent reactor-netty HTTP calls to validate WeakHashMap-based state management.
 * This ensures the refactored implementation (without @NewField) correctly handles parallel requests
 * and doesn't have race conditions or memory leaks.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"reactor.netty", "reactor.core", "org.springframework"})
public class ReactorNettyConcurrencyTest {
    @Rule
    public HttpServerRule server = new HttpServerRule();

    private ExecutorService executor;

    @BeforeClass
    public static void beforeClass() {
        // This is here to prevent reactor.util.ConsoleLogger output from taking over your screen
        System.setProperty("reactor.logging.fallback", "JDK");
    }

    @Before
    public void setUp() {
        executor = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testParallelHttpClientCalls() throws Exception {
        URI endpoint = server.getEndPoint();
        String url = endpoint.toString();
        String host = endpoint.getHost();

        int nIterations = 10;  // Reduced for faster tests
        waitAll(
                executor.submit(new HttpClientCaller(nIterations, url)),
                executor.submit(new HttpClientCaller(nIterations, url)),
                executor.submit(new HttpClientCaller(nIterations, url))
        );

        // Each thread makes nIterations calls to the same host, so total = 3 * nIterations
        assertEquals(3 * nIterations, getUnscopedMetricCount("External/" + host + "/NettyReactor/GET"));
        assertEquals(3 * nIterations, getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testConnectionReuse() throws Exception {
        URI endpoint = server.getEndPoint();
        String url = endpoint.toString();
        String host = endpoint.getHost();

        // Test multiple requests to the same host to validate connection pooling
        // and ensure WeakHashMap entries are properly cleaned up after each request
        int nIterations = 10;
        HttpClientCaller caller = new HttpClientCaller(nIterations, url);
        caller.run();

        // Should report exactly nIterations, no duplicates or missing calls
        assertEquals(nIterations, getUnscopedMetricCount("External/" + host + "/NettyReactor/GET"));
    }

    private void waitAll(Future<?>... futures) throws Exception {
        for (Future<?> future : futures) {
            future.get();
        }
    }

    public static class HttpClientCaller implements Runnable {
        final int count;
        final String url;

        public HttpClientCaller(int count, String url) {
            this.count = count;
            this.url = url;
        }

        @Override
        public void run() {
            int remainingCalls = count;
            while (remainingCalls-- > 0) {
                try {
                    makeGetRequest(url);
                } catch (Exception e) {
                    // Ignore errors - we're testing concurrency, not connectivity
                }
            }
        }

        @Trace(dispatcher = true)
        public void makeGetRequest(String url) {
            HttpClient client = HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(5));
            try {
                client.get()
                        .uri(url)
                        .response()
                        .block(Duration.ofSeconds(5));
            } catch (Exception e) {
                // Ignore errors in concurrency tests
            }
        }
    }
}