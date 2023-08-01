package com.newrelic.agent.stats;

import org.junit.Test;

import static org.junit.Assert.*;

public class StatsImplTest {

    @Test
    public void testClone() throws CloneNotSupportedException{
        StatsImpl stats = new StatsImpl(2, 3.12f, 5.01f, 8.95f, 13.02);
        StatsImpl statsClone = (StatsImpl) stats.clone();
        double delta = .00001;


        assertNotSame(stats, statsClone);
        assertEquals(2, statsClone.getCallCount());
        assertEquals(3.12, statsClone.getTotal(), delta);
        assertEquals(5.01, statsClone.getMinCallTime(), delta);
        assertEquals(8.95, statsClone.getMaxCallTime(), delta);
        assertEquals(13.02, statsClone.getSumOfSquares(), delta);

    }

    @Test
    public void testToString(){
        StatsImpl stats = new StatsImpl(2, 3.12f, 5.01f, 8.95f, 13.02);
        String suffix = "[total=3.12, count=2, minValue=5.01, maxValue=8.95, sumOfSquares=13.02]";
        assertTrue(stats.toString().endsWith(suffix));

    }

    @Test(expected = IllegalArgumentException.class)
    public void recordDataPoint_throws_whenSOSTooLarge(){
        double maxDouble = Double.MAX_VALUE;
        StatsImpl stats = new StatsImpl(2, 3.12f, 5.01f, 8.95f, maxDouble);
        stats.recordDataPoint(Float.MAX_VALUE);
    }

    @Test
    public void testHasData(){
        StatsImpl stats = new StatsImpl(1, 2.2f, 4.007f, 8.12f, 75.68);
        assertTrue(stats.hasData());

        stats.reset();
        assertFalse(stats.hasData());

        StatsImpl statsSomeData = new StatsImpl(0, 2.2f, 3f, 4f, 6.6);
        StatsImpl statsSomeOtherData = new StatsImpl(4, 0, 3f, 4f, 6.6);
        assertTrue(statsSomeData.hasData());
        assertTrue(statsSomeOtherData.hasData());
    }

}