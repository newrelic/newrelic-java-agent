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
import org.quartz.core.SchedulingContext;

@Weave(type = MatchType.Interface, originalName = "org.quartz.spi.JobStore")
public class QuartzSystem_Instrumentation {

    @Trace(dispatcher = true)
    public Trigger acquireNextTrigger(SchedulingContext context, long noLaterThan) {

        return Weaver.callOriginal();
    }
}
