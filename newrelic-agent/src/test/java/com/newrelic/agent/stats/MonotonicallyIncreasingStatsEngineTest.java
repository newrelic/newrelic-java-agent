/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import org.junit.Assert;
import org.junit.Test;

public class MonotonicallyIncreasingStatsEngineTest {

    @Test
    public void test() {
        MonotonicallyIncreasingStatsEngine monoStatsEngine = new MonotonicallyIncreasingStatsEngine();

        StatsEngine statsEngine = new StatsEngineImpl();
        String name = "dude";
        monoStatsEngine.recordMonoStats(statsEngine, name, 100); // value is 100
        monoStatsEngine.recordMonoStats(statsEngine, name, 150); // value is 50
        monoStatsEngine.recordMonoStats(statsEngine, name, 200); // value is 50
        monoStatsEngine.recordMonoStats(statsEngine, name, 210); // value is 10

        Assert.assertEquals(210f, statsEngine.getStats(name).getTotal(), 0);
        Assert.assertEquals(4, statsEngine.getStats(name).getCallCount(), 0);
    }

    @Test
    public void moreThanOne() {
        MonotonicallyIncreasingStatsEngine monoStatsEngine = new MonotonicallyIncreasingStatsEngine();

        StatsEngine statsEngine = new StatsEngineImpl();
        String name = "dude";
        String name2 = "dude2";
        monoStatsEngine.recordMonoStats(statsEngine, name, 100); // value is 100
        monoStatsEngine.recordMonoStats(statsEngine, name, 150); // value is 50
        monoStatsEngine.recordMonoStats(statsEngine, name, 200); // value is 50
        monoStatsEngine.recordMonoStats(statsEngine, name, 210); // value is 10

        monoStatsEngine.recordMonoStats(statsEngine, name2, 0); // value is 0
        monoStatsEngine.recordMonoStats(statsEngine, name2, 0); // value is 0
        monoStatsEngine.recordMonoStats(statsEngine, name2, 10); // value is 10
        monoStatsEngine.recordMonoStats(statsEngine, name2, 10); // value is 10

        Assert.assertEquals(210f, statsEngine.getStats(name).getTotal(), 0);
        Assert.assertEquals(4, statsEngine.getStats(name).getCallCount());

        Assert.assertEquals(10f, statsEngine.getStats(name2).getTotal(), 0);
        Assert.assertEquals(4, statsEngine.getStats(name2).getCallCount());
    }
}
