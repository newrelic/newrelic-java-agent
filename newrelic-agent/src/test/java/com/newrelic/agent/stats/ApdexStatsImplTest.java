package com.newrelic.agent.stats;

import com.newrelic.agent.model.ApdexPerfZone;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mockStatic;

public class ApdexStatsImplTest {

    @Test
    public void testClone() throws CloneNotSupportedException{
        ApdexStatsImpl apdexStats = new ApdexStatsImpl(3, 5, 8);
        ApdexStatsImpl clone = (ApdexStatsImpl) apdexStats.clone();

        assertEquals(3, clone.getApdexSatisfying());
        assertEquals(5, clone.getApdexTolerating());
        assertEquals(8, clone.getApdexFrustrating());

        assertNotEquals(apdexStats.toString(), clone.toString());
        assertNotSame(clone, apdexStats);

    }

    @Test
    public void testHasData() {
        ApdexStatsImpl apdexStatsNoData = new ApdexStatsImpl(0, 0, 0);
        ApdexStatsImpl apdexStatsSomeData1 = new ApdexStatsImpl(1, 0, 0);
        ApdexStatsImpl apdexStatsSomeData2 = new ApdexStatsImpl(0, 2, 0);
        ApdexStatsImpl apdexStatsSomeData3 = new ApdexStatsImpl(0, 0, 1);
        ApdexStatsImpl apdexStatsAllTheData = new ApdexStatsImpl(1, 2, 3);

        assertFalse(apdexStatsNoData.hasData());
        assertTrue(apdexStatsSomeData1.hasData());
        assertTrue(apdexStatsSomeData2.hasData());
        assertTrue(apdexStatsSomeData3.hasData());
        assertTrue(apdexStatsAllTheData.hasData());

    }

    @Test
    public void testReset() {
        ApdexStatsImpl apdexStats = new ApdexStatsImpl(1,2 , 3);
        apdexStats.reset();
        assertFalse(apdexStats.hasData());
    }

    @Test
    public void recordApdexResponseTime_correctlyIncrements_perfZones(){
        ApdexStatsImpl apdexStats = new ApdexStatsImpl(3, 5, 8);
        try (MockedStatic<ApdexPerfZoneDetermination> mockStaticApdexPerfZoneDet = mockStatic(ApdexPerfZoneDetermination.class)){

            //The responseTime/adpexT combos below align with the current thresholds for Satisfying, Tolerating, and Frustrating in ApdexPerfZoneDetermination.
            //Tested with MockedStatic so that this test will pass if the thresholds for the zones ever change.
            mockStaticApdexPerfZoneDet.when(()-> ApdexPerfZoneDetermination.getZone(10L, 40L)).thenReturn(ApdexPerfZone.SATISFYING);
            mockStaticApdexPerfZoneDet.when(()-> ApdexPerfZoneDetermination.getZone(150L, 40L)).thenReturn(ApdexPerfZone.TOLERATING);
            mockStaticApdexPerfZoneDet.when(()-> ApdexPerfZoneDetermination.getZone(200L, 40L)).thenReturn(ApdexPerfZone.FRUSTRATING);

            apdexStats.recordApdexResponseTime(10L, 40L);
            assertEquals(4, apdexStats.getApdexSatisfying());
            assertEquals(5, apdexStats.getApdexTolerating());
            assertEquals(8, apdexStats.getApdexFrustrating());

            apdexStats.recordApdexResponseTime(150L, 40L);
            assertEquals(4, apdexStats.getApdexSatisfying());
            assertEquals(6, apdexStats.getApdexTolerating());
            assertEquals(8, apdexStats.getApdexFrustrating());

            apdexStats.recordApdexResponseTime(200L, 40L);
            assertEquals(4, apdexStats.getApdexSatisfying());
            assertEquals(6, apdexStats.getApdexTolerating());
            assertEquals(9, apdexStats.getApdexFrustrating());
        }

    }

    @Test
    public void merge_nonApdexStats_doesNothing(){
        ApdexStatsImpl apdexStats = new ApdexStatsImpl(3, 5, 8);
        StatsImpl notApdexStatsObj = new StatsImpl(1, 2, 3, 4, 5);
        apdexStats.merge(notApdexStatsObj);

        assertEquals(3, apdexStats.getApdexSatisfying());
        assertEquals(5, apdexStats.getApdexTolerating());
        assertEquals(8, apdexStats.getApdexFrustrating());

    }

    @Test
    public void merge_apdexStats_incrementsAllFields(){
        ApdexStatsImpl apdexStats = new ApdexStatsImpl(3, 5, 8);
        ApdexStatsImpl otherApdexStats = new ApdexStatsImpl(10, 11, 12);
        apdexStats.merge(otherApdexStats);

        assertEquals(13, apdexStats.getApdexSatisfying());
        assertEquals(16, apdexStats.getApdexTolerating());
        assertEquals(20, apdexStats.getApdexFrustrating());
    }

}