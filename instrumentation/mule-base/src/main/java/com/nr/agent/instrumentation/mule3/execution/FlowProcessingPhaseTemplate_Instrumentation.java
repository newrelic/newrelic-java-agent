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
 * Template methods for MessageSource specific behavior during flow execution.
 */
@Weave(type = MatchType.Interface, originalName = "org.mule.execution.FlowProcessingPhaseTemplate")
public abstract class FlowProcessingPhaseTemplate_Instrumentation {

    /**
     * Pre processing of the {@link MuleEvent} to route
     *
     * @param muleEvent
     */
    @Trace
    public MuleEvent beforeRouteEvent(MuleEvent muleEvent) {
        return Weaver.callOriginal();
    }

    /**
     * Routes the {@link MuleEvent} through the processors chain
     *
     * @param muleEvent {@link MuleEvent} created from the raw message of this context
     * @return the response {@link MuleEvent}
     */
    @Trace
    public MuleEvent routeEvent(MuleEvent muleEvent) {
        return Weaver.callOriginal();
    }

    /**
     * Post processing of the routed {@link MuleEvent}
     *
     * @param muleEvent
     */
    @Trace
    public MuleEvent afterRouteEvent(MuleEvent muleEvent) {
        return Weaver.callOriginal();
    }

    /**
     * Call after successfully processing the message through the flow
     * This method will always be called when the flow execution was successful.
     *
     * @param muleEvent
     */
    @Trace
    public void afterSuccessfulProcessingFlow(MuleEvent muleEvent) {
        Weaver.callOriginal();
    }

}
