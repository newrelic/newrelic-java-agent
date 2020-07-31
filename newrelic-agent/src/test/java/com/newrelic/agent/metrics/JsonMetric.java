/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

import org.json.simple.JSONArray;
import org.junit.Assert;

import com.newrelic.agent.stats.ResponseTimeStatsImpl;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.util.TimeConversion;

/*
 * This was ported over from the critical path branch. I am leaving the critical path in the json. However, the
 * Java agent is not actually calculating the critical path. Therefore the value is never 
 * actually validated.
 */
public class JsonMetric {

    private String fileName;
    private String testName;
    private String metricName;
    private String scope;
    private long count;
    private long totalTimeMs;
    private long exclusivetimeMs;
    private long criticalPathMs;
    private Long minMs;
    private Long maxMs;

    public static JsonMetric createMetric(JSONArray metricArray, String testname, String pFileName) {
        if (metricArray.size() != 6 && metricArray.size() != 8) {
            throw new IllegalArgumentException("A metric array should have 6 pr 8 positions and not "
                    + metricArray.size());
        }

        JsonMetric metric = new JsonMetric();
        metric.fileName = pFileName;
        metric.metricName = (String) metricArray.get(0);
        metric.scope = (String) metricArray.get(1);
        metric.count = (Long) metricArray.get(2);
        metric.totalTimeMs = (Long) metricArray.get(3);
        metric.exclusivetimeMs = (Long) metricArray.get(4);
        metric.criticalPathMs = (Long) metricArray.get(5);
        Assert.assertTrue("Exclusive time should always be greater than or equal to critical path time.",
                metric.exclusivetimeMs >= metric.criticalPathMs);

        if (metricArray.size() == 8) {
            metric.minMs = (Long) metricArray.get(6);
            metric.maxMs = (Long) metricArray.get(7);
        }

        metric.testName = testname;

        return metric;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getScope() {
        return scope;
    }

    public long getCount() {
        return count;
    }

    public long getTotalTime() {
        return totalTimeMs;
    }

    public long getExclusivetime() {
        return exclusivetimeMs;
    }

    public long getCriticalPath() {
        return criticalPathMs;
    }

    public void validateMetricExists(StatsBase stats) {
        // these two have already been verified
        // Assert.assertEquals(transactionName, scope);
        // Assert.assertEquals(mName, metricName);
        Assert.assertTrue(stats instanceof ResponseTimeStatsImpl);
        ResponseTimeStatsImpl impl = (ResponseTimeStatsImpl) stats;
        String name = fileName + ", \"" + testName + "\", " + metricName + ", Invalid ";
        Assert.assertEquals(name + "call count", count, impl.getCallCount());
        Assert.assertEquals(name + "total time", totalTimeMs, impl.getTotal() * TimeConversion.MILLISECONDS_PER_SECOND,
                .001);
        Assert.assertEquals(name + "exclusive time", exclusivetimeMs, impl.getTotalExclusiveTime()
                * TimeConversion.MILLISECONDS_PER_SECOND, .001);

        if (minMs != null) {
            Assert.assertEquals(name + "min time", minMs, impl.getMinCallTime()
                    * TimeConversion.MILLISECONDS_PER_SECOND, .001);
        } else if (impl.getCallCount() == 1) {
            Assert.assertEquals(name + "min time", totalTimeMs, impl.getMinCallTime()
                    * TimeConversion.MILLISECONDS_PER_SECOND, .001);
        }
        if (maxMs != null) {
            Assert.assertEquals(name + "max time", maxMs, impl.getMaxCallTime()
                    * TimeConversion.MILLISECONDS_PER_SECOND, .001);
        } else if (impl.getCallCount() == 1) {
            Assert.assertEquals(name + "max time", totalTimeMs, impl.getMaxCallTime()
                    * TimeConversion.MILLISECONDS_PER_SECOND, .001);
        }
    }

}
