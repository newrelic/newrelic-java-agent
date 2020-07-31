/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.utilization.UtilizationConfig;

public class UtilizationConfigTest {

    @Test
    public void testNoConfig() {
        UtilizationConfig utilConfig = new UtilizationConfig(null, null, null);
        Assert.assertNull(utilConfig.getHostname());
        Assert.assertNull(utilConfig.getLogicalProcessors());
        Assert.assertNull(utilConfig.getTotalRamMib());

    }

    @Test
    public void testConfigData() throws IOException {
        UtilizationConfig utilConfig = new UtilizationConfig("dude", 16, 1024L);
        Assert.assertEquals("dude", utilConfig.getHostname());
        Assert.assertEquals((Integer) 16, utilConfig.getLogicalProcessors());
        Assert.assertEquals(new Long(1024), utilConfig.getTotalRamMib());
    }
}
