/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class TransactionTimerTest {

    @Test
    public void testTransactionTimerStartEnd() throws InterruptedException {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());

        Thread.sleep(2);

        long endTime = System.nanoTime();
        timer.markTransactionActivityAsDone(endTime, endTime - startTime);

        // activity complete but transaction is not
        Assert.assertEquals(endTime, timer.getEndTimeInNanos());
        Assert.assertEquals(0, timer.getResponseTimeInNanos());
        Assert.assertTrue((endTime - startTime) < timer.getRunningDurationInNanos());
        Assert.assertEquals((endTime - startTime), timer.getTotalSumTimeInNanos());

        // end transaction
        timer.markTransactionAsDone();
        Assert.assertEquals(endTime, timer.getEndTimeInNanos());
        Assert.assertEquals(endTime - startTime, timer.getResponseTimeInNanos());
        Assert.assertEquals(endTime - startTime, timer.getRunningDurationInNanos());
        Assert.assertEquals((endTime - startTime), timer.getTotalSumTimeInNanos());
    }

    @Test
    public void testTotalTime() {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());

        timer.markTransactionActivityAsDone(System.nanoTime(), 10);
        Assert.assertEquals(10, timer.getTotalSumTimeInNanos());

        timer.markTransactionActivityAsDone(System.nanoTime(), 100);
        Assert.assertEquals(110, timer.getTotalSumTimeInNanos());

        timer.markTransactionActivityAsDone(System.nanoTime(), 1);
        Assert.assertEquals(111, timer.getTotalSumTimeInNanos());

        timer.markTransactionActivityAsDone(System.nanoTime(), 112);
        Assert.assertEquals(223, timer.getTotalSumTimeInNanos());
    }

    @Test
    public void testTransactionTimerGetRunningDurationInNanos() throws InterruptedException {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());

        long time1 = timer.getRunningDurationInNanos();

        Thread.sleep(2);

        long time2 = timer.getRunningDurationInNanos();
        Assert.assertTrue(time2 > time1);

        long endTime = System.nanoTime();
        Assert.assertEquals(0, timer.getEndTimeInNanos());
        timer.markTransactionActivityAsDone(endTime, 45);
        Assert.assertEquals(endTime, timer.getEndTimeInNanos());

        Thread.sleep(1);
        Assert.assertTrue((endTime - startTime) < timer.getRunningDurationInNanos());

        endTime = System.nanoTime();
        timer.markTransactionActivityAsDone(endTime, 45);
        Assert.assertEquals(endTime, timer.getEndTimeInNanos());

        Thread.sleep(1);
        Assert.assertTrue((endTime - startTime) < timer.getRunningDurationInNanos());

        /*
         * In practice the running duration should never get shorter like it does in this test. If we are grabbing a
         * running duration, then we should either be within the transaction or we are RUM meaning the responseTime will
         * be set with the last byte.
         */
        timer.markTransactionAsDone();
        Assert.assertEquals((endTime - startTime), timer.getRunningDurationInNanos());
    }

    @Test
    public void testTransactionTimerGetRunningDurationWithWebTx() throws InterruptedException {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());

        long time1 = timer.getRunningDurationInNanos();

        Thread.sleep(2);

        long time2 = timer.getRunningDurationInNanos();
        Assert.assertTrue(time2 > time1);

        long endTime = System.nanoTime();
        Assert.assertEquals(0, timer.getEndTimeInNanos());
        timer.markResponseTime(endTime);

        Thread.sleep(1);
        Assert.assertEquals((endTime - startTime), timer.getRunningDurationInNanos());

        timer.markTransactionAsDone();
        Assert.assertEquals((endTime - startTime), timer.getRunningDurationInNanos());
    }

    @Test
    public void testEndTransaction() throws InterruptedException {
        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());
        Thread.sleep(2);
        long firstTime = timer.getRunningDurationInNanos();
        Thread.sleep(2);
        long endTime = System.nanoTime();
        timer.markTimeToLastByte(endTime);
        timer.markResponseTime(endTime);
        timer.markTransactionAsDone();
        long duration = timer.getResponseTimeInNanos();
        long lastByteDur = timer.getTimetoLastByteInNanos();
        Assert.assertTrue(firstTime < duration);
        Assert.assertEquals((endTime - startTime), duration);
        Assert.assertEquals(duration, timer.getRunningDurationInNanos());
        Assert.assertEquals(lastByteDur, duration);

        // second mark should be ignored
        timer.markTimeToLastByte(endTime);
        long duration2 = timer.getResponseTimeInNanos();
        Assert.assertEquals(duration2, duration);
        Assert.assertEquals(duration, timer.getTimetoLastByteInNanos());
    }

    @Test
    public void testMarkTxAsDoneAtEnd() throws Exception {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());

        Thread.sleep(1);

        long txa1 = System.nanoTime();
        timer.markTransactionActivityAsDone(txa1, 5);
        Assert.assertEquals(5, timer.getTotalSumTimeInNanos());

        Thread.sleep(1);

        long txa2 = System.nanoTime();
        timer.markTransactionActivityAsDone(txa2, 15);
        Assert.assertEquals(20, timer.getTotalSumTimeInNanos());

        Thread.sleep(1);

        long txa3 = System.nanoTime();
        timer.markTransactionActivityAsDone(txa3, 2);
        Assert.assertEquals(22, timer.getTotalSumTimeInNanos());

        Thread.sleep(1);

        Assert.assertTrue((txa3 - startTime) < timer.getRunningDurationInNanos());
        timer.markTransactionAsDone();

        long dur = txa3 - startTime;
        Assert.assertEquals(dur, timer.getResponseTimeInNanos());
        Assert.assertEquals(dur, timer.getRunningDurationInNanos());
        Assert.assertEquals(dur, timer.getTransactionDurationInNanos());
        Assert.assertEquals(0, timer.getTimetoLastByteInNanos());
        Assert.assertEquals(0, timer.getTimeToFirstByteInNanos());
        Assert.assertEquals(22, timer.getTotalSumTimeInNanos());
    }

    @Test
    public void testTransactionDurationBackground() throws Exception {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());
        Thread.sleep(1);
        long txa1 = System.nanoTime();
        timer.markTransactionActivityAsDone(txa1, 5);
        Assert.assertEquals(5, timer.getTotalSumTimeInNanos());
        Thread.sleep(1);
        long txa2 = System.nanoTime();
        timer.markTransactionActivityAsDone(txa2, 15);
        Assert.assertEquals(20, timer.getTotalSumTimeInNanos());

        long expectedMs = TimeUnit.MILLISECONDS.convert(txa2 - startTime, TimeUnit.NANOSECONDS);
        timer.markTransactionAsDone();
        Assert.assertEquals((txa2 - startTime), timer.getTransactionDurationInNanos());
        Assert.assertEquals(expectedMs, timer.getTransactionDurationInMilliseconds());
        Assert.assertEquals(expectedMs, timer.getResponseTimeInMilliseconds());
    }

    @Test
    public void testTransactionDurationWeb() throws Exception {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());
        Thread.sleep(1);
        long txa1 = System.nanoTime();
        Assert.assertTrue(timer.markTimeToFirstByte(txa1));
        Assert.assertFalse(timer.markTimeToFirstByte(System.nanoTime()));
        timer.markTransactionActivityAsDone(txa1, 5);
        Assert.assertEquals(5, timer.getTotalSumTimeInNanos());
        Thread.sleep(1);
        long response = System.nanoTime();
        Assert.assertTrue(timer.markTimeToLastByte(response));
        Assert.assertFalse(timer.markTimeToLastByte(response));
        Assert.assertTrue(timer.markResponseTime(response));
        Thread.sleep(1);
        long txa2 = System.nanoTime();
        timer.markTransactionActivityAsDone(txa2, 15);
        Assert.assertEquals(20, timer.getTotalSumTimeInNanos());

        long expectedWallClockMs = TimeUnit.MILLISECONDS.convert(txa2 - startTime, TimeUnit.NANOSECONDS);
        Assert.assertEquals(expectedWallClockMs, timer.getTransactionDurationInMilliseconds());

        timer.markTransactionAsDone();
        long expectedResponseMs = TimeUnit.MILLISECONDS.convert(response - startTime, TimeUnit.NANOSECONDS);
        Assert.assertEquals(expectedResponseMs, timer.getResponseTimeInMilliseconds());

        Assert.assertEquals(txa1 - startTime, timer.getTimeToFirstByteInNanos());
        Assert.assertEquals(response - startTime, timer.getTimetoLastByteInNanos());
    }

    @Test
    public void testFirstLastByteOrder() throws Exception {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());
        Thread.sleep(1);
        long txa1 = System.nanoTime();
        Assert.assertTrue(timer.markTimeToLastByte(txa1));
        Assert.assertFalse(timer.markTimeToLastByte(System.nanoTime()));
        Assert.assertFalse(timer.markTimeToFirstByte(System.nanoTime()));

        Assert.assertEquals(0, timer.getTimeToFirstByteInNanos());
        long response = txa1 - startTime;
        Assert.assertEquals(response, timer.getTimetoLastByteInNanos());
    }

    @Test
    public void testFirstLastByteSameNoResponseSet() throws Exception {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());
        Thread.sleep(1);
        long txa1 = System.nanoTime();
        Assert.assertTrue(timer.markTimeToFirstByte(txa1));
        Assert.assertFalse(timer.markTimeToFirstByte(System.nanoTime()));
        Assert.assertTrue(timer.markTimeToLastByte(txa1));

        long response = txa1 - startTime;
        Assert.assertEquals(response, timer.getTimeToFirstByteInNanos());
        Assert.assertEquals(response, timer.getTimetoLastByteInNanos());
        Assert.assertEquals(0, timer.getResponseTimeInNanos());
        Assert.assertTrue(response < timer.getRunningDurationInNanos());
    }

    @Test
    public void testFirstLastByteSame() throws Exception {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());
        Thread.sleep(1);
        long txa1 = System.nanoTime();
        Assert.assertTrue(timer.markTimeToFirstByte(txa1));
        Assert.assertFalse(timer.markTimeToFirstByte(System.nanoTime()));
        Assert.assertTrue(timer.markTimeToLastByte(txa1));
        Assert.assertTrue(timer.markResponseTime(txa1));
        Assert.assertFalse(timer.markResponseTime(System.nanoTime()));

        long response = txa1 - startTime;
        Assert.assertEquals(response, timer.getTimeToFirstByteInNanos());
        Assert.assertEquals(response, timer.getTimetoLastByteInNanos());
        Assert.assertEquals(response, timer.getResponseTimeInNanos());
        Assert.assertEquals(response, timer.getRunningDurationInNanos());

    }

    @Test
    public void testFirstLastByteDifferent() throws Exception {

        long startTime = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTime);
        Assert.assertEquals(startTime, timer.getStartTimeInNanos());
        Thread.sleep(1);
        long firstTime = System.nanoTime();
        Assert.assertTrue(timer.markTimeToFirstByte(firstTime));
        Assert.assertFalse(timer.markTimeToFirstByte(System.nanoTime()));

        Thread.sleep(1);
        long responseTime = System.nanoTime();
        Assert.assertTrue(timer.markTimeToLastByte(responseTime));
        Assert.assertFalse(timer.markTimeToLastByte(System.nanoTime()));

        Thread.sleep(1);
        long txa = System.nanoTime();
        timer.markTransactionActivityAsDone(txa, 2);
        timer.markTransactionAsDone();

        long response = responseTime - startTime;
        Assert.assertEquals(firstTime - startTime, timer.getTimeToFirstByteInNanos());
        Assert.assertEquals(response, timer.getTimetoLastByteInNanos());
        Assert.assertEquals(txa - startTime, timer.getResponseTimeInNanos());
        Assert.assertEquals(2, timer.getTotalSumTimeInNanos());
        Assert.assertEquals(txa - startTime, timer.getTransactionDurationInNanos());
    }

    @Test
    public void testMarkResponseSent() throws Exception {
        final long startTimestamp = System.nanoTime();
        TransactionTimer timer = new TransactionTimer(startTimestamp);
        Assert.assertEquals(startTimestamp, timer.getStartTimeInNanos());

        Thread.sleep(1);

        long txa1 = System.nanoTime();
        timer.markTransactionActivityAsDone(txa1, 5);
        Assert.assertEquals(5, timer.getTotalSumTimeInNanos());

        final long responseTimestamp = System.nanoTime();
        final long expectedResponseTime = responseTimestamp - startTimestamp;
        Assert.assertTrue(timer.markResponseTime(responseTimestamp));
        Assert.assertEquals(expectedResponseTime, timer.getResponseTimeInNanos());
        Assert.assertFalse(timer.markResponseTime(responseTimestamp+66));

        Thread.sleep(1);

        final long txa2 = System.nanoTime();
        timer.markTransactionActivityAsDone(txa2, 15);
        Assert.assertEquals(20, timer.getTotalSumTimeInNanos());
        Assert.assertEquals(expectedResponseTime, timer.getResponseTimeInNanos());

        Thread.sleep(1);

        Assert.assertTrue((txa2 - startTimestamp) > timer.getRunningDurationInNanos());
        timer.markTransactionAsDone();
        Assert.assertFalse(timer.markResponseTime(12345));

        final long dur = txa2 - startTimestamp;
        Assert.assertEquals(expectedResponseTime, timer.getResponseTimeInNanos());
        Assert.assertEquals(expectedResponseTime, timer.getRunningDurationInNanos());
        Assert.assertEquals(dur, timer.getTransactionDurationInNanos());
        Assert.assertEquals(0, timer.getTimetoLastByteInNanos());
        Assert.assertEquals(0, timer.getTimeToFirstByteInNanos());
        Assert.assertEquals(20, timer.getTotalSumTimeInNanos());
    }

}
