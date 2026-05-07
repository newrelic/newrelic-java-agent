/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.newrelic.api.agent.CloudAccountInfo.AWS_ACCOUNT_ID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudAccountInfoCacheTest {

    @Test
    public void accountInfo() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();

        assertNull(cache.getAccountInfo(AWS_ACCOUNT_ID));

        String accountId = "123456789012";
        cache.setAccountInfo(AWS_ACCOUNT_ID, accountId);

        assertEquals(accountId, cache.getAccountInfo(AWS_ACCOUNT_ID));
    }

    @Test
    public void accountInfoClient() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        Object sdkClient = new Object();

        assertNull(cache.getAccountInfo(sdkClient, AWS_ACCOUNT_ID));

        String accountId = "123456789012";
        cache.setAccountInfo(sdkClient, AWS_ACCOUNT_ID, accountId);

        assertEquals(accountId, cache.getAccountInfo(sdkClient, AWS_ACCOUNT_ID));

        Object anotherSdkClient = new Object();
        assertNull(cache.getAccountInfo(anotherSdkClient, AWS_ACCOUNT_ID));
    }

    @Test
    public void accountInfoClientFallback() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        String accountId = "123456789012";
        cache.setAccountInfo(AWS_ACCOUNT_ID, accountId);

        Object sdkClient = new Object();
        assertEquals(accountId, cache.getAccountInfo(sdkClient, AWS_ACCOUNT_ID));
    }


    @Test
    public void retrieveDataFromConfigAccountInfo() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        String accountId = "123456789012";

        ServiceManager serviceManager = mock(ServiceManager.class, RETURNS_DEEP_STUBS);
        ServiceFactory.setServiceManager(serviceManager);
        when(serviceManager.getConfigService().getDefaultAgentConfig().getValue("cloud.aws.account_id"))
                .thenReturn(accountId);
        cache.retrieveDataFromConfig();

        assertEquals(accountId, cache.getAccountInfo(AWS_ACCOUNT_ID));
    }

    @Test
    public void cacheCanGrowBeyondInitialCapacity() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        List<Object> clients = new ArrayList<>();

        // Initial capacity is 4, add more than 4 clients to verify cache can grow
        for (int i = 0; i < 10; i++) {
            Object client = new Object();
            clients.add(client);
            String accountId = String.format("10000000000%d", i); // 12 digits: 10000000000X
            cache.setAccountInfo(client, AWS_ACCOUNT_ID, accountId);
            assertEquals("Cache should store entry " + i, accountId, cache.getAccountInfo(client, AWS_ACCOUNT_ID));
        }

        // Verify all entries are still accessible
        for (int i = 0; i < 10; i++) {
            String expectedAccountId = String.format("10000000000%d", i);
            assertEquals("Cache should maintain entry " + i, expectedAccountId,
                    cache.getAccountInfo(clients.get(i), AWS_ACCOUNT_ID));
        }
    }

    @Test
    public void concurrentAccessFromMultipleThreads() throws InterruptedException {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        int threadCount = 10;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Multiple threads setting/getting with different clients concurrently
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int i = 0; i < operationsPerThread; i++) {
                        Object client = new Object();
                        // Generate unique 12-digit account ID: format as 20TTIIIIIIII (TT=thread, IIIIIIII=operation)
                        String accountId = String.format("20%02d%08d", threadId, i);

                        cache.setAccountInfo(client, AWS_ACCOUNT_ID, accountId);
                        String retrieved = cache.getAccountInfo(client, AWS_ACCOUNT_ID);

                        if (accountId.equals(retrieved)) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);

        executor.shutdown();
        assertTrue("All threads should complete within timeout", completed);
        assertEquals("All operations should succeed", threadCount * operationsPerThread, successCount.get());
    }

    @Test
    public void multipleSdkClientsWithDistinctAccountInfo() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();

        Object client1 = new Object();
        Object client2 = new Object();
        Object client3 = new Object();

        cache.setAccountInfo(client1, AWS_ACCOUNT_ID, "111111111111");
        cache.setAccountInfo(client2, AWS_ACCOUNT_ID, "222222222222");
        cache.setAccountInfo(client3, AWS_ACCOUNT_ID, "333333333333");

        // Each client should maintain its own account info
        assertEquals("111111111111", cache.getAccountInfo(client1, AWS_ACCOUNT_ID));
        assertEquals("222222222222", cache.getAccountInfo(client2, AWS_ACCOUNT_ID));
        assertEquals("333333333333", cache.getAccountInfo(client3, AWS_ACCOUNT_ID));

        // Clients should not interfere with each other
        Object client4 = new Object();
        assertNull(cache.getAccountInfo(client4, AWS_ACCOUNT_ID));
    }

    @Test
    public void nullClientHandling() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        Object nullClient = null;

        // Setting with null client should be ignored
        cache.setAccountInfo(nullClient, AWS_ACCOUNT_ID, "999999999999");

        // Getting with null client should use NULL_CLIENT fallback
        assertNull(cache.getAccountInfo(nullClient, AWS_ACCOUNT_ID));

        // Set global default
        cache.setAccountInfo(AWS_ACCOUNT_ID, "888888888888");

        // Null client should fall back to global
        assertEquals("888888888888", cache.getAccountInfo(nullClient, AWS_ACCOUNT_ID));
    }

    @Test
    public void removeAccountInfoBySettingNull() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        Object client = new Object();

        cache.setAccountInfo(client, AWS_ACCOUNT_ID, "444444444444");
        assertEquals("444444444444", cache.getAccountInfo(client, AWS_ACCOUNT_ID));

        // Setting null should remove the entry
        cache.setAccountInfo(client, AWS_ACCOUNT_ID, null);
        assertNull(cache.getAccountInfo(client, AWS_ACCOUNT_ID));
    }
}