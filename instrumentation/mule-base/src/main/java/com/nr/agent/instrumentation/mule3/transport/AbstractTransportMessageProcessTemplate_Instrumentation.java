/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.transport;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.mule3.MuleUtils;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiver;

@Weave(type = MatchType.BaseClass, originalName = "org.mule.transport.AbstractTransportMessageProcessTemplate")
public abstract class AbstractTransportMessageProcessTemplate_Instrumentation<MessageReceiverType extends AbstractMessageReceiver, ConnectorType extends AbstractConnector> {

    @Trace
    public void afterFailureProcessingFlow(MessagingException messagingException) {
        AgentBridge.privateApi.reportException(messagingException);
        MuleUtils.handleException(messagingException.getEvent());
        Weaver.callOriginal();
    }

    @Trace
    public void afterFailureProcessingFlow(MuleException exception) {
        AgentBridge.privateApi.reportException(exception);
        Weaver.callOriginal();
    }

    /**
     * Pre processing of the {@link MuleEvent} to route
     *
     * @param muleEvent
     */
    @Trace
    public MuleEvent beforeRouteEvent(MuleEvent muleEvent) {
        String className = this.getClass().getName();
        if (className != null && className.equals("org.mule.transport.http.HttpMessageProcessTemplate")) {
            MuleUtils.reportToAgent(muleEvent);
        }

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
