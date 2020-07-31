/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Test;

public class AgentTestUtilsTest {
    @Test
    public void testAssertVariance() {
        AgentTestUtils.assertVariance(1, 2, 1);
        AgentTestUtils.assertVariance(2, 1, .5);
        AgentTestUtils.assertVariance(10, 10, 0);
        AgentTestUtils.assertVariance(9, 10, .2);
        AgentTestUtils.assertVariance(10, 9, .1);
        AgentTestUtils.assertVariance(6, 10, .67);
        AgentTestUtils.assertVariance(10, 6, .4);
    }
}
