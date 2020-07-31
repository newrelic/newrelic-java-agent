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

public class StatsTest {

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
        StatsImpl stats = new StatsImpl();
        for (int i = 0; i < 2; i++) {
            stats.recordDataPoint(66);
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
        for (int i = 0; i < 2; i++) {
            stats.recordResponseTime(66000, 44000, TimeUnit.MILLISECONDS);
        }

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(2l, array.get(0));
        Assert.assertEquals(132, getInt(array.get(1)));
        Assert.assertEquals(88, getInt(array.get(2)));
    }

    @Test
    public void basicStatsJsonOnlyCallCount() throws Exception {
        StatsImpl stats = new StatsImpl();
        stats.setCallCount(666);

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(666l, array.get(0));
    }

    @Test
    public void basicStatsJsonCallCountOne() throws Exception {
        StatsImpl stats = new StatsImpl();
        stats.recordDataPoint(66);

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(1l, array.get(0));
        Assert.assertEquals(66, getInt(array.get(1)));
    }

    @Test
    public void basicStatsJsonCallCountOneDifferentExclusiveTime() throws Exception {
        ResponseTimeStatsImpl stats = new ResponseTimeStatsImpl();
        stats.recordResponseTime(66000, 44000, TimeUnit.MILLISECONDS);

        JSONArray array = (JSONArray) serialize(stats);
        Assert.assertEquals(6, array.size());
        Assert.assertEquals(1l, array.get(0));
        Assert.assertEquals(66, getInt(array.get(1)));
        Assert.assertEquals(44, getInt(array.get(2)));
    }

    @Test
    public void merge() {
        StatsImpl stats = new StatsImpl();
        stats.recordDataPoint(100);

        stats.merge(new StatsImpl());

        Assert.assertEquals(100f, stats.getMinCallTime(), 0);
        Assert.assertEquals(100f, stats.getMaxCallTime(), 0);

        StatsImpl otherStats = new StatsImpl();
        otherStats.merge(stats);

        Assert.assertEquals(100f, otherStats.getMinCallTime(), 0);
        Assert.assertEquals(100f, otherStats.getMaxCallTime(), 0);

        otherStats.recordDataPoint(20);

        Assert.assertEquals(20f, otherStats.getMinCallTime(), 0);

        otherStats.merge(stats);

        Assert.assertEquals(20f, otherStats.getMinCallTime(), 0);

        stats.merge(otherStats);
        Assert.assertEquals(20f, stats.getMinCallTime(), 0);
    }

    @Test
    public void mergeIncompatibleType() {
        StatsImpl stats = new StatsImpl();
        stats.recordDataPoint(100);

        ResponseTimeStatsImpl otherStats = new ResponseTimeStatsImpl();
        otherStats.recordResponseTime(500L, TimeUnit.MILLISECONDS);

        stats.merge(otherStats);

        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(1, otherStats.getCallCount());
    }

    private int getInt(Object val) {
        return ((Number) val).intValue();
    }

    public static Stats createStats() {
        return new StatsImpl();
    }

    public static Stats createStats(StatsEngine statsEngine) {
        return new StatsImpl();
    }
}
