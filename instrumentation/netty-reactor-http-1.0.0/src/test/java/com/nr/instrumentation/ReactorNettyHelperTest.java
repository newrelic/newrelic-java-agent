/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

import com.newrelic.api.agent.Segment;
import org.junit.Test;
import reactor.netty.Connection;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Lifecycle primitive tests for ReactorNettyContext and ReactorNettyHelper.
 */
public class ReactorNettyHelperTest {

    /**
     * When ReactorNettyContext.put() displaces an existing SegmentData, the prior Segment must
     * have end() called on it so it stops pinning its parent Transaction in runningChildren.
     */
    @Test
    public void testPutEndsPriorSegmentOnOverwrite() {
        Connection conn = mock(Connection.class);
        Segment first = mock(Segment.class);
        Segment second = mock(Segment.class);

        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(first, URI.create("http://a/"), "GET"));
        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(second, URI.create("http://b/"), "POST"));

        verify(first, times(1)).end();
        verify(second, never()).end();
    }

    /**
     * When put() does NOT displace an existing entry, no segment should be ended.
     */
    @Test
    public void testPutWithoutPriorIsCleanInsert() {
        Connection conn = mock(Connection.class);
        Segment only = mock(Segment.class);

        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(only, URI.create("http://a/"), "GET"));

        verify(only, never()).end();
    }

    /**
     * The put() method must safely ignore null connection or null segmentData without throwing an exception.
     */
    @Test
    public void testPutNullArgsIsNoOp() {
        ReactorNettyContext.put(null, new ReactorNettyContext.SegmentData(mock(Segment.class), URI.create("http://x/"), "GET"));
        ReactorNettyContext.put(mock(Connection.class), null);
        ReactorNettyContext.put(null, null);
    }

    /**
     * cleanupOrphanedSegment must remove the entry AND call Segment.end() on lingering Segments.
     * This is what RESPONSE_COMPLETED, DISCONNECTING, RELEASED and onUncaughtException delegate to.
     */
    @Test
    public void testCleanupOrphanedSegmentEndsLingeringSegment() {
        Connection conn = mock(Connection.class);
        Segment segment = mock(Segment.class);

        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(segment, URI.create("http://x/"), "GET"));
        ReactorNettyHelper.cleanupOrphanedSegment(conn);

        verify(segment, times(1)).end();
        assertNull("Map entry should be removed after cleanup", ReactorNettyContext.remove(conn));
    }

    /**
     * cleanupOrphanedSegment on a Connection with no map entry must be a safe no-op, not a NullPointerException.
     */
    @Test
    public void testCleanupOrphanedSegmentNoEntryIsNoOp() {
        Connection conn = mock(Connection.class);
        ReactorNettyHelper.cleanupOrphanedSegment(conn);
    }

    /**
     * ReactorNettyContext.remove() returns the previously stored entry and removes it from the map.
     */
    @Test
    public void testRemoveReturnsAndRemovesEntry() {
        Connection conn = mock(Connection.class);
        Segment segment = mock(Segment.class);
        ReactorNettyContext.SegmentData data = new ReactorNettyContext.SegmentData(segment, URI.create("http://z/"), "GET");

        ReactorNettyContext.put(conn, data);
        ReactorNettyContext.SegmentData removed = ReactorNettyContext.remove(conn);

        assertSame(data, removed);
        assertNull("Entry should be gone after remove", ReactorNettyContext.remove(conn));

        verify(segment, never()).end();
    }

    /**
     * SegmentData constructed with a null Segment should not cause cleanupOrphanedSegment
     * to throw an NPE.
     */
    @Test
    public void testCleanupOrphanedSegmentWithNullSegmentInData() {
        Connection conn = mock(Connection.class);
        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(null, URI.create("http://x/"), "GET"));
        ReactorNettyHelper.cleanupOrphanedSegment(conn);

        assertNull("Entry should still be removed even if segment was null", ReactorNettyContext.remove(conn));
    }

    /**
     * Defends against a put-overwrite of a SegmentData whose Segment field is null.
     */
    @Test
    public void testPutOverwriteWithNullPriorSegmentIsClean() {
        Connection conn = mock(Connection.class);
        Segment newSegment = mock(Segment.class);

        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(null, URI.create("http://a/"), "GET"));
        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(newSegment, URI.create("http://b/"), "POST"));

        verify(newSegment, never()).end();
    }

    /**
     * cleanupOrphanedSegment must be safe to call multiple times. The same Connection can have cleanup fire multiple times in sequence.
     * Only one call should end the segment.
     */
    @Test
    public void testCleanupOrphanedSegmentIsIdempotent() {
        Connection conn = mock(Connection.class);
        Segment segment = mock(Segment.class);
        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(segment, URI.create("http://x/"), "GET"));

        ReactorNettyHelper.cleanupOrphanedSegment(conn);
        ReactorNettyHelper.cleanupOrphanedSegment(conn);   // second fire should be safe no-op

        verify(segment, times(1)).end();
        assertNull(ReactorNettyContext.remove(conn));
    }

    /**
     * Under concurrent put and cleanup on the same Connection, every Segment must be ended exactly once.
     */
    @Test
    public void testConcurrentPutAndCleanupEndsAllSegments() throws InterruptedException {
        final int threadCount = 16;
        final Connection conn = mock(Connection.class);
        final List<Segment> segments = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            segments.add(mock(Segment.class));
        }

        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threadCount);
        final ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final Segment mine = segments.get(i);
            pool.submit(() -> {
                try {
                    start.await();
                    ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(mine, URI.create("http://t/"), "GET"));
                    ReactorNettyHelper.cleanupOrphanedSegment(conn);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue("Test threads did not complete in time", done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        for (int i = 0; i < threadCount; i++) {
            verify(segments.get(i), times(1)).end();
        }

        assertNull("Map should be empty after all threads finish", ReactorNettyContext.remove(conn));
    }

    /**
     * reactor-netty pools connections so the same Connection instance can be reused later in unrelated transactions. Each transaction's Segment must end at the
     * right time with no cross talk between transactions.
     */
    @Test
    public void testCrossTransactionConnectionReuse() {
        Connection conn = mock(Connection.class);
        Segment txnASegment = mock(Segment.class);
        Segment txnBSegment = mock(Segment.class);

        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(txnASegment, URI.create("http://a/"), "GET"));
        ReactorNettyHelper.cleanupOrphanedSegment(conn);

        verify(txnASegment, times(1)).end();
        verify(txnBSegment, never()).end();
        assertNull("Map should be empty after txn A finishes", ReactorNettyContext.remove(conn));

        ReactorNettyContext.put(conn, new ReactorNettyContext.SegmentData(txnBSegment, URI.create("http://b/"), "POST"));
        ReactorNettyHelper.cleanupOrphanedSegment(conn);

        verify(txnASegment, times(1)).end();
        verify(txnBSegment, times(1)).end();
        assertNull("Map should be empty after txn B finishes", ReactorNettyContext.remove(conn));
    }
}