/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.execution;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.mule.execution.FlowProcessingPhaseTemplate;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.PhaseResultNotifier;

/**
 * This phase routes the message through the flow.
 */
@Weave(type = MatchType.ExactClass, originalName = "org.mule.execution.FlowProcessingPhase")
public abstract class FlowProcessingPhase_Instrumentation {

    @Trace(excludeFromTransactionTrace = true)
    public void runPhase(final FlowProcessingPhaseTemplate flowProcessingPhaseTemplate,
            final MessageProcessContext messageProcessContext,
            final PhaseResultNotifier phaseResultNotifier) {
        Weaver.callOriginal();
    }

}
