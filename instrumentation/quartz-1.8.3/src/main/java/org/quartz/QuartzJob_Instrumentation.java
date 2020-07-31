/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.quartz;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.quartz.Job")
public class QuartzJob_Instrumentation {

    @Trace(dispatcher = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            if (context.getJobDetail() != null) {
                NewRelic.addCustomParameter("name", context.getJobDetail().getFullName());
            }
        } catch (Throwable e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "An error occurred getting a Quartz job name");
        }
        Weaver.callOriginal();
    }
}
