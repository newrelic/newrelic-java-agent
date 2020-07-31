/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.transport;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.mule3.MuleUtils;
import org.mule.api.MuleEvent;
import org.mule.api.source.MessageSource;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.MessageProcessTemplate;

/**
 * AbstractMessageReceiver provides common methods for all Message Receivers provided with Mule.
 * A message receiver enables an endpoint to receive a message from an external system.
 * <p>
 * Instrumentation for any message (web- or non-web) running through Mule.
 */
@Weave(type = MatchType.BaseClass, originalName = "org.mule.transport.AbstractMessageReceiver")
public abstract class AbstractMessageReceiver_Implementation {

    @Trace(dispatcher = true)
    protected void processMessage(final MessageProcessTemplate messageProcessTemplate, final MessageProcessContext messageProcessContext) {
        MessageSource messageSource = messageProcessContext.getMessageSource();
        if (messageSource instanceof DefaultInboundEndpoint) {
            DefaultInboundEndpoint endpoint = (DefaultInboundEndpoint) messageSource;

            final Transaction txn = AgentBridge.getAgent().getTransaction();

            // Normally, there will be only one visit to an inboundEndpoint per request, but in case this is nested,
            // we only set transaction state on the outer-most one.
            if (((com.newrelic.agent.bridge.TracedMethod) txn.getTracedMethod()).getParentTracedMethod() == null) {
                final String protocol = endpoint.getConnector().getProtocol().toUpperCase();
                final String path = endpoint.getEndpointURI().getPath();
                final String txnName = protocol + path;
                txn.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "Mule", txnName);
            }
        }

        Weaver.callOriginal();
    }

    @Trace(async = true)
    public MuleEvent routeEvent(MuleEvent muleEvent) {
        Object tokenKey = muleEvent.getFlowVariable(MuleUtils.MULE_EVENT_TOKEN_KEY);
        if (tokenKey == null) {
            final Transaction txn = AgentBridge.getAgent().getTransaction(false);
            if (txn != null) {
                Token token = txn.getToken();
                if (token != null) {
                    MuleUtils.registerToken(muleEvent, token);
                }
            }
        }

        return Weaver.callOriginal();
    }

}
