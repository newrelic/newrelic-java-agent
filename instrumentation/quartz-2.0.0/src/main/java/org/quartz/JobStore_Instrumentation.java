/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.quartz;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.quartz.spi.OperableTrigger;

import java.util.List;

@Weave(type = MatchType.Interface, originalName = "org.quartz.spi.JobStore")
public class JobStore_Instrumentation {

    @Trace(dispatcher = true)
    public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) {
        return Weaver.callOriginal();
    }
}
