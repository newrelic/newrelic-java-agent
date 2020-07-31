/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ws.server;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.springws20.SpringWSUtils;

@Weave(type = MatchType.BaseClass)
public class MessageDispatcher {

    protected void processEndpointException(MessageContext messageContext, Object endpoint, Exception ex) {
        Weaver.callOriginal();
        AgentBridge.privateApi.reportException(ex);
    }

    public void receive(MessageContext messageContext) {
        WebServiceMessage message = messageContext.getRequest();
        if (message instanceof SaajSoapMessage) {
            SpringWSUtils.nameTransaction((SaajSoapMessage) message);
            SpringWSUtils.addCustomAttributes((SoapMessage) message);
        }
        Weaver.callOriginal();
    }
}
