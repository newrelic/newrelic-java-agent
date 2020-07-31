/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.test.repro2;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.introspec.internal.HttpServerLocator;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
 * This test verifies that an issue that occurs with servlets + spring async + async http clients where an inconsistent state error
 * would occur and cause the transaction to be split into many parts.
 */
public class SpringBootAsyncTest {

    public static final ExecutorService executorService = new ThreadPoolExecutor(100, 200, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1000),
            new ThreadFactoryBuilder().build(), new ThreadPoolExecutor.AbortPolicy());

    private static final ExecutorService executor = Executors.newFixedThreadPool(8);

    private static final TransactionDataList transactions = new TransactionDataList();

    @ClassRule
    public static HttpServerRule SERVER = new HttpServerRule();

    private static URI ENDPOINT;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServiceFactory.getTransactionService().addTransactionListener(transactions);
        ENDPOINT = SERVER.getEndPoint();
    }

    @Before
    public void setup() {
        transactions.clear();
    }

    @Test
    public void testInconsistentState() {
        ConfigurableApplicationContext context = SpringApplication.run(AsyncResource.class);
        transactions.clear(); // Ignore the servlet init() transactions

        try {
            Collection<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 250; i++) {
                futures.add(executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8090/" + ENDPOINT.getPort()).openConnection();
                            int responseCode = connection.getResponseCode();
                            assertEquals(200, responseCode);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }));
            }

            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            TransactionDataList list = transactions.waitFor(250, TimeUnit.SECONDS.toMillis(30));
            Set<String> uniqueTxNames = new HashSet<>(list.getTransactionNames());
            assertEquals(1, uniqueTxNames.size());
            assertTrue(uniqueTxNames.contains("WebTransaction/SpringController/{port} (GET)"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
            executorService.shutdownNow();
            context.close();
        }
    }

}
