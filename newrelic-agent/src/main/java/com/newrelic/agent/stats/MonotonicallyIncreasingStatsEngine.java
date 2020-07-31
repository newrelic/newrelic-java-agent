/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import java.util.HashMap;
import java.util.Map;

public class MonotonicallyIncreasingStatsEngine {

    private final Map<String, MonotonicallyIncreasingStatsHelper> monoStatsHelpers = new HashMap<>();

    public void recordMonoStats(StatsEngine statsEngine, String name, float value) {
        MonotonicallyIncreasingStatsHelper monoStatsHelper = getMonotonicallyIncreasingStatsHelper(name);
        Stats stats = statsEngine.getStats(name);
        monoStatsHelper.recordDataPoint(stats, value);
    }

    private MonotonicallyIncreasingStatsHelper getMonotonicallyIncreasingStatsHelper(String name) {
        MonotonicallyIncreasingStatsHelper monoStatsHelper = monoStatsHelpers.get(name);
        if (monoStatsHelper == null) {
            monoStatsHelper = new MonotonicallyIncreasingStatsHelper();
            monoStatsHelpers.put(name, monoStatsHelper);
        }
        return monoStatsHelper;
    }

    private class MonotonicallyIncreasingStatsHelper {

        private float lastValue = 0f;

        public MonotonicallyIncreasingStatsHelper() {
        }

        public void recordDataPoint(Stats stats, float value) {
            if (lastValue > value) {
                lastValue = 0;
            }
            stats.recordDataPoint(value - lastValue);
            lastValue = value;
        }
    }

}
