/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.test.repro1;

import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/*
 * This test verifies that an issue in Tomcat 8.5.0 (used in spring boot) where an async transaction would show up as
 * two separate transactions is fixed. It verifies this by counting the number of transactions and verifying the name
 */
public class SpringBootTest {

    //@Test
    public void testDuplicateTransactions() throws Exception {
        final AtomicInteger txCounter = new AtomicInteger(0);
        final AtomicInteger finishedTxCount = new AtomicInteger(0);
        final AtomicReference<String> finishedTxString = new AtomicReference<>();

        try (ConfigurableApplicationContext context = SpringApplication.run(ArticleResource.class)) {
            ServiceFactory.getTransactionService().addTransactionListener(
                    new ExtendedTransactionListener() {
                        @Override
                        public void dispatcherTransactionStarted(Transaction transaction) {
                            txCounter.incrementAndGet();
                        }

                        @Override
                        public void dispatcherTransactionCancelled(Transaction transaction) {
                            // no-op
                        }

                        @Override
                        public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
                            txCounter.decrementAndGet();
                            if (transactionData.getBlameMetricName().startsWith("OtherTransaction")) {
                                return;
                            }

                            finishedTxCount.incrementAndGet();
                            finishedTxString.set(transactionData.getBlameMetricName());
                        }
                    });

            int port = (int) context.getBean("port");
            HttpURLConnection connection = (HttpURLConnection) new URL("http", "localhost", port, "/").openConnection();
            int responseCode = connection.getResponseCode();

            assertEquals(200, responseCode);

            int timeout = 10; // 20 * 250ms = 5 seconds
            while (timeout > 0 && txCounter.get() > 0) {
                Thread.sleep(250);
                timeout--;
            }

            assertEquals(1, finishedTxCount.get());
            assertEquals("WebTransaction/SpringController/ (GET)", finishedTxString.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
