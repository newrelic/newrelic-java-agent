package com.newrelic.agent.stats;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleStatsEngineTest {

    SimpleStatsEngine eng;
    @Before
    public void setup(){
        this.eng = new SimpleStatsEngine(10);
    }

    @Test(expected=RuntimeException.class)
    public void getStats_nullMetrics_shouldThrow(){
        eng.getStats(null);
    }

    @Test(expected=RuntimeException.class)
    public void getStats_metricWithWrongStatsType_shouldThrow(){
        String dummyMetric = "fake_metric";
        eng.recordEmptyStats(dummyMetric);
        eng.getStats(dummyMetric);
    }

    @Test(expected=RuntimeException.class)
    public void getOrCreateResponseTimeStats_nullMetric_shouldThrow(){
        eng.getOrCreateResponseTimeStats(null);
    }

    @Test(expected=RuntimeException.class)
    public void getOrCreateResponseTimeStats_metricWithWrongStatsType_shouldThrow(){
        String dummyMetric = "fake_metric";
        eng.recordEmptyStats(dummyMetric);
        eng.getOrCreateResponseTimeStats(dummyMetric);
    }

    @Test(expected=RuntimeException.class)
    public void recordEmptyStats_nullMetric_shouldThrow(){
        eng.recordEmptyStats(null);
    }

    @Test(expected=RuntimeException.class)
    public void getApdexStats_nullMetric_shouldThrow(){
        eng.getApdexStats(null);
    }

    @Test(expected=RuntimeException.class)
    public void getApdexStats_metricWithWrongStatsType_shouldThrow(){
        String dummyMetric = "fake_metric";
        eng.recordEmptyStats(dummyMetric);
        eng.getApdexStats(dummyMetric);
    }

    @Test(expected=RuntimeException.class)
    public void getDataUsageStats_nullMetric_shouldThrow(){
        eng.getDataUsageStats(null);
    }

    @Test(expected=RuntimeException.class)
    public void getDataUsageStats_metricWithWrongStatsType_shouldThrow(){
        String dummyMetric = "fake_metric";
        eng.recordEmptyStats(dummyMetric);
        eng.getDataUsageStats(dummyMetric);
    }

    @Test
    public void testToString(){
        eng.recordEmptyStats("foo");
        ResponseTimeStats responseTimeStats = eng.getOrCreateResponseTimeStats("baz");
        String statsString = eng.getStatsMap().toString();
        assertEquals("SimpleStatsEngine [stats=" + statsString + "]", eng.toString());

    }



}