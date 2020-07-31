/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;
import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.AgentHelper;

public class ResponseTimeStatsTest {

    private static Object serialize(JSONStreamAware obj) throws Exception {
        return AgentHelper.serializeJSON(obj);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notANumber() throws Exception {
        StatsImpl stats = new StatsImpl();
        stats.recordDataPoint(Float.NaN);
    }

    @Test
    public void basicStatsJsonMultipleCalls() throws Exception {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        long duration = TimeUnit.NANOSECONDS.convert(66000, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 2; i++) {
            stats.recordResponseTime(duration, TimeUnit.NANOSECONDS);
        }

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(2l, array.get(0));
        Assert.assertEquals(132, getInt(array.get(1)));
        Assert.assertEquals(132.0d, array.get(2));
        Assert.assertEquals(66l, getInt(array.get(3)));
        Assert.assertEquals(66l, getInt(array.get(4)));
        Assert.assertEquals(66l * 66 * 2, getInt(array.get(5)));
    }

    @Test
    public void basicStatsJsonMultipleCallsInMilliseconds() throws Exception {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        long duration = 66000L;
        for (int i = 0; i < 2; i++) {
            stats.recordResponseTime(duration, TimeUnit.MILLISECONDS);
        }

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(2l, array.get(0));
        Assert.assertEquals(132, getInt(array.get(1)));
        Assert.assertEquals(132.0d, array.get(2));
        Assert.assertEquals(66l, getInt(array.get(3)));
        Assert.assertEquals(66l, getInt(array.get(4)));
        Assert.assertEquals(66l * 66 * 2, getInt(array.get(5)));
    }

    @Test
    public void basicStatsJsonMultipleCallsDifferentExclusiveTime() throws Exception {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        long duration = TimeUnit.NANOSECONDS.convert(66000, TimeUnit.MILLISECONDS);
        long exclusiveDuration = TimeUnit.NANOSECONDS.convert(44000, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 2; i++) {
            stats.recordResponseTime(duration, exclusiveDuration, TimeUnit.NANOSECONDS);
        }

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(2l, array.get(0));
        Assert.assertEquals(132, getInt(array.get(1)));
        Assert.assertEquals(88, getInt(array.get(2)));
    }

    @Test
    public void basicStatsJsonOnlyCallCount() throws Exception {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        stats.setCallCount(666);

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(666l, array.get(0));
    }

    @Test
    public void basicStatsJsonCallCountOne() throws Exception {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        long duration = TimeUnit.NANOSECONDS.convert(66000, TimeUnit.MILLISECONDS);
        stats.recordResponseTime(duration, TimeUnit.NANOSECONDS);

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(1l, array.get(0));
        Assert.assertEquals(66, getInt(array.get(1)));
    }

    @Test
    public void basicStatsJsonCallCountOneDifferentExclusiveTime() throws Exception {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        long duration = TimeUnit.NANOSECONDS.convert(66000, TimeUnit.MILLISECONDS);
        long exclusiveDuration = TimeUnit.NANOSECONDS.convert(44000, TimeUnit.MILLISECONDS);
        stats.recordResponseTime(duration, exclusiveDuration, TimeUnit.NANOSECONDS);

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(1l, array.get(0));
        Assert.assertEquals(66, getInt(array.get(1)));
        Assert.assertEquals(44, getInt(array.get(2)));
    }

    @Test
    public void merge() {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        long duration = TimeUnit.NANOSECONDS.convert(100000, TimeUnit.MILLISECONDS);
        stats.recordResponseTime(duration, TimeUnit.NANOSECONDS);

        stats.merge(new StatsImpl());

        Assert.assertEquals(100f, stats.getMinCallTime(), 0);
        Assert.assertEquals(100f, stats.getMaxCallTime(), 0);

        ResponseTimeStatsImpl otherStats = new ResponseTimeStatsImpl();
        otherStats.merge(stats);

        Assert.assertEquals(100f, otherStats.getMinCallTime(), 0);
        Assert.assertEquals(100f, otherStats.getMaxCallTime(), 0);

        duration = TimeUnit.NANOSECONDS.convert(20000, TimeUnit.MILLISECONDS);
        otherStats.recordResponseTime(duration, TimeUnit.NANOSECONDS);

        Assert.assertEquals(20f, otherStats.getMinCallTime(), 0);

        otherStats.merge(stats);

        Assert.assertEquals(20f, otherStats.getMinCallTime(), 0);

        stats.merge(otherStats);
        Assert.assertEquals(20f, stats.getMinCallTime(), 0);
    }

    @Test
    public void sumOfSquares() throws Exception {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        int count = 10;
        long total = 2000;
        long min = 1000;
        long max = 4000;
        stats.recordResponseTime(count, total, min, max, TimeUnit.MILLISECONDS);
        Assert.assertEquals(10, stats.getCallCount());
        Assert.assertEquals(2.0f, stats.getTotal(), 0);
        Assert.assertEquals(1.0f, stats.getMinCallTime(), 0);
        Assert.assertEquals(4.0f, stats.getMaxCallTime(), 0);
        Assert.assertEquals(4.0d, stats.getSumOfSquares(), 0);
    }

    private int getInt(Object val) {
        return ((Number) val).intValue();
    }

    public static Stats createStats() {
        return new StatsImpl();
    }
}
