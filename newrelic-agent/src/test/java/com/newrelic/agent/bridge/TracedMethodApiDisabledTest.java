/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import org.junit.Test;

import com.newrelic.api.agent.NewRelic;

/**
 * Tests to assert that calling TracedMethod apis without the agent does not throw NPEs.
 */
public class TracedMethodApiDisabledTest {

    @Test
    public void setMetricNameInternal() {
        AgentBridge.getAgent().getTracedMethod().setMetricName("one", "two");
    }

    @Test
    public void setMetricNamePublic() {
        NewRelic.getAgent().getTracedMethod().setMetricName("one", "two");
    }
}
