/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.util.Caffeine2CollectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

// AI assisted test generation
public class TimedTokenSetTest {

    private ExpirationService mockExpirationService;
    private TimedTokenSet timedTokenSet;

    @Before
    public void setUp() {
        mockExpirationService = mock(ExpirationService.class);
        // Set up a real collection factory that supports expiration and removal listeners
        AgentBridge.collectionFactory = new Caffeine2CollectionFactory();
    }

    @After
    public void tearDown() {
        // Reset to avoid affecting other tests
        AgentBridge.collectionFactory = new com.newrelic.agent.bridge.DefaultCollectionFactory();
    }

    @Test
    public void testPutAndRemove() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token = createMockToken();

        timedTokenSet.put(token);
        assertTrue("Token should be removable after being added", timedTokenSet.remove(token));
        assertFalse("Token should not be removable twice", timedTokenSet.remove(token));
    }

    @Test
    public void testRemoveNonExistentToken() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token = createMockToken();

        assertFalse("Removing non-existent token should return false", timedTokenSet.remove(token));
    }

    @Test
    public void testRemoveAll() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token1 = createMockToken();
        TokenImpl token2 = createMockToken();
        TokenImpl token3 = createMockToken();

        timedTokenSet.put(token1);
        timedTokenSet.put(token2);
        timedTokenSet.put(token3);

        timedTokenSet.removeAll();

        // All tokens should be removed
        assertFalse("Token1 should be removed after removeAll", timedTokenSet.remove(token1));
        assertFalse("Token2 should be removed after removeAll", timedTokenSet.remove(token2));
        assertFalse("Token3 should be removed after removeAll", timedTokenSet.remove(token3));
    }

    @Test
    public void testRefresh() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token = createMockToken();
        timedTokenSet.put(token);

        // Refresh should not throw exception
        timedTokenSet.refresh(token);

        // Token should still be removable after refresh
        assertTrue("Token should still be present after refresh", timedTokenSet.remove(token));
    }

    @Test
    public void testCleanUp() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token = createMockToken();
        timedTokenSet.put(token);

        // cleanUp should not throw exception
        timedTokenSet.cleanUp();

        // Token should still be removable after cleanUp
        assertTrue("Token should still be present after cleanUp", timedTokenSet.remove(token));
    }

    @Test
    public void testInitialTimedOutCount() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        assertEquals("Initial timed out count should be 0", 0, timedTokenSet.timedOutCount());
    }

    @Test
    public void testExplicitRemovalDoesNotIncrementTimeout() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token = createMockToken();

        timedTokenSet.put(token);
        timedTokenSet.remove(token);

        // Explicit removal should not count as timeout
        assertEquals("Explicit removal should not increment timeout count", 0, timedTokenSet.timedOutCount());
    }

    @Test
    public void testMinimumTimeoutBound() {
        // Test that 0 timeout gets converted to minimum (250ms)
        // This should not throw an exception
        timedTokenSet = new TimedTokenSet(0, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token = createMockToken();
        timedTokenSet.put(token);

        assertEquals("Token should not timeout immediately with 0 timeout", 0, timedTokenSet.timedOutCount());
    }

    @Test
    public void testMultipleTokensCanBeStored() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token1 = createMockToken();
        TokenImpl token2 = createMockToken();
        TokenImpl token3 = createMockToken();

        timedTokenSet.put(token1);
        timedTokenSet.put(token2);
        timedTokenSet.put(token3);

        // All should be removable
        assertTrue("Token1 should be removable", timedTokenSet.remove(token1));
        assertTrue("Token2 should be removable", timedTokenSet.remove(token2));
        assertTrue("Token3 should be removable", timedTokenSet.remove(token3));
    }

    @Test
    public void testSameTokenCanBeReAdded() {
        timedTokenSet = new TimedTokenSet(60, TimeUnit.SECONDS, mockExpirationService);

        TokenImpl token = createMockToken();

        timedTokenSet.put(token);
        assertTrue("Token should be removable first time", timedTokenSet.remove(token));

        // Re-add the same token
        timedTokenSet.put(token);
        assertTrue("Token should be removable after re-adding", timedTokenSet.remove(token));
    }

    /**
     * Helper method to create a mock token with basic setup
     */
    private TokenImpl createMockToken() {
        TokenImpl token = mock(TokenImpl.class);
        WeakRefTransaction mockWeakRef = mock(WeakRefTransaction.class);
        Transaction mockTransaction = mock(Transaction.class);

        when(token.getTransaction()).thenReturn(mockWeakRef);
        when(mockWeakRef.getTransactionIfExists()).thenReturn(mockTransaction);

        return token;
    }
}