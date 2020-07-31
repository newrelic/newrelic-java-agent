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
import org.mule.api.MuleEvent;

/**
 * Extension of FlowProcessingPhaseTemplate for those org.mule.api.source.MessageSource
 * that requires sending a response of the message processed.
 */
@Weave(type = MatchType.Interface, originalName = "org.mule.execution.RequestResponseFlowProcessingPhaseTemplate")
public abstract class RequestResponseFlowProcessingPhaseTemplate_Instrumentation {

    /**
     * Template method to send a response after processing the message. This method is executed within the flow so if
     * it fails it will trigger the exception strategy.
     */
    @Trace
    public void sendResponseToClient(MuleEvent muleEvent) {
        Weaver.callOriginal();
    }

}
