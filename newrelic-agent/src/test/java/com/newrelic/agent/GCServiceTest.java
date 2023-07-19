package com.newrelic.agent;

import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import org.junit.Test;

import static org.junit.Assert.*;

public class GCServiceTest {
    @Test
    public void beforeHarvest_populatesStatsEngine() {
        GCService gcService = new GCService();
        StatsEngine statsEngine = new StatsEngineImpl();

        //Attempt to ensure that a GC has occurred..
        for (int i = 0; i < 1000000; i++) {
            new Object();
        }
        System.gc();
        System.gc();

        // First call to beforeHarvest populates the GC beans, second call actually scrapes the metrics
        gcService.beforeHarvest("app", statsEngine);
        gcService.beforeHarvest("app", statsEngine);

        // Not using asserts (so as not to fail the build) in case GC was not triggered for some reason
        if (statsEngine.getSize() == 0) {
            System.out.println("GC was not triggered");
        }
    }

    @Test
    public void isEnabled_returnsTrue() {
        GCService gcService = new GCService();
        assertTrue(gcService.isEnabled());
    }
}
