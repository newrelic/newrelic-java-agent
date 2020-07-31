/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BasicSupportabilityMetricsTest {

    @Test
    @Ignore
    public void testLoadedClassesMetricCaptured() throws Exception {
        // clear out any metric data
        InstrumentTestUtils.getAndClearMetricData();

        // Load a class that hasn't yet been loaded to see if we capture supportability metrics about it
        ClassLoader.getSystemClassLoader().loadClass("test.newrelic.test.agent.DummyClass");

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // we don't test for exact counts here since we might have loaded other classes
        assertTrue("It contains the key:", metricData.containsKey("Supportability/LoadedClasses/java/1.6/count"));
        assertFalse("And does not contain the key::", metricData.containsKey("Supportability/LoadedClasses/unknown/1.8/count"));
    }
}
