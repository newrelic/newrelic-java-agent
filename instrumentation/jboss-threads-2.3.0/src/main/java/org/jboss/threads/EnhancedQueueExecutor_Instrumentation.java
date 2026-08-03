/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.jboss.threads;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.JBossThreadsUtils;
import com.nr.agent.instrumentation.NRRunnable;

@Weave(originalName = "org.jboss.threads.EnhancedQueueExecutor", type = MatchType.ExactClass)
public class EnhancedQueueExecutor_Instrumentation {
    public void execute(Runnable runnable) {
        NRRunnable wrapper = JBossThreadsUtils.getWrapper(runnable);

        if(wrapper != null) {
            runnable = wrapper;
        }

        Weaver.callOriginal();
    }
}
