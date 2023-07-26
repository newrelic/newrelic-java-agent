package com.newrelic.agent.stats;

import org.junit.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class DataUsageStatsTest {
    @Test
    public void clone_properlyClonesObject() throws CloneNotSupportedException {
        DataUsageStats dataUsageStats = new DataUsageStatsImpl();
        dataUsageStats.recordDataUsage(100L, 200L);
        dataUsageStats.recordDataUsage(100L, 200L);

        //Count: 2, sent 200, received: 400
        DataUsageStats clone = (DataUsageStats)dataUsageStats.clone();
        assertEquals(2, clone.getCount());
        assertEquals(200L, clone.getBytesSent());
        assertEquals(400L, clone.getBytesReceived());
    }

    @Test
    public void hasData_returnsTrueWithDataPresent() {
        DataUsageStats dataUsageStats = new DataUsageStatsImpl();
        dataUsageStats.recordDataUsage(100L, 200L);
        assertTrue(dataUsageStats.hasData());

        dataUsageStats = new DataUsageStatsImpl();
        assertFalse(dataUsageStats.hasData());
    }

    @Test
    public void reset_setAllValuesToZero() {
        DataUsageStats dataUsageStats = new DataUsageStatsImpl();
        dataUsageStats.recordDataUsage(100L, 200L);
        assertTrue(dataUsageStats.hasData());

        dataUsageStats.reset();
        assertFalse(dataUsageStats.hasData());
        assertEquals(0, dataUsageStats.getCount());
        assertEquals(0, dataUsageStats.getBytesReceived());
        assertEquals(0, dataUsageStats.getBytesSent());
    }

    @Test
    public void writeJSONString_writesToSuppliedWriter() throws IOException {
        DataUsageStats dataUsageStats = new DataUsageStatsImpl();
        dataUsageStats.recordDataUsage(100L, 200L);

        try (MockedStatic<org.json.simple.JSONArray> mockJsonArray = mockStatic(org.json.simple.JSONArray.class)) {
            dataUsageStats.writeJSONString(mock(Writer.class));
            mockJsonArray.verify(() -> org.json.simple.JSONArray.writeJSONString(any(), any()));
        }
    }
}
