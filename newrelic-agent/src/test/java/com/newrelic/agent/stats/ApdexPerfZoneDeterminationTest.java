/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import org.junit.Test;

import static com.newrelic.agent.model.ApdexPerfZone.FRUSTRATING;
import static com.newrelic.agent.model.ApdexPerfZone.SATISFYING;
import static com.newrelic.agent.model.ApdexPerfZone.TOLERATING;
import static com.newrelic.agent.stats.ApdexPerfZoneDetermination.getZone;
import static org.junit.Assert.assertEquals;

public class ApdexPerfZoneDeterminationTest {

    @Test
    public void testSatisfying() throws Exception {
        assertEquals(SATISFYING, getZone(100, 100));
        assertEquals(SATISFYING, getZone(99, 100));
    }

    @Test
    public void testTolerating() throws Exception {
        assertEquals(TOLERATING, getZone(101, 100));
        assertEquals(TOLERATING, getZone(100, 25));
    }

    @Test
    public void testFrustrating() throws Exception {
        assertEquals(FRUSTRATING, getZone(50000, 100));
        assertEquals(FRUSTRATING, getZone(401, 100));
    }

}