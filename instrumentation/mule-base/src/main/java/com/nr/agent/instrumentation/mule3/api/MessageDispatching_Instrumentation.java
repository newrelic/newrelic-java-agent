/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.api;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;

/**
 * Define generic methods for dispatching events.
 * The exact behaviour of the action is defined by the implementing class.
 */
@Weave(type = MatchType.Interface, originalName = "org.mule.api.transport.MessageDispatching")
public abstract class MessageDispatching_Instrumentation {

    /**
     * Dispatches an event from the endpoint to the external system
     *
     * @param event The event to dispatch
     * @throws DispatchException if the event fails to be dispatched
     */
    @Trace(excludeFromTransactionTrace = true)
    public void dispatch(MuleEvent event) {
        Weaver.callOriginal();
    }

    /**
     * Sends an event from the endpoint to the external system
     *
     * @param event The event to send
     * @return event the response form the external system wrapped in a MuleEvent
     * @throws DispatchException if the event fails to be dispatched
     */
    @Trace(excludeFromTransactionTrace = true)
    public MuleMessage send(MuleEvent event) {
        return Weaver.callOriginal();
    }

}
