/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import java.util.Set;

import org.junit.Test;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MetricNames;

public class MemoryTest {

    @Test
    public void testMemoryMetrics() throws Exception {

        synchronized (this) {
            this.wait(4000);
        }

        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyMetrics(metrics, MetricNames.MEMORY, MetricNames.MEMORY_USED, MetricNames.MEMORY,
                MetricNames.HEAP_COMMITTED, MetricNames.HEAP_MAX, MetricNames.HEAP_USED,
                MetricNames.NON_HEAP_COMMITTED, MetricNames.NON_HEAP_MAX, MetricNames.NON_HEAP_USED);
    }
}
