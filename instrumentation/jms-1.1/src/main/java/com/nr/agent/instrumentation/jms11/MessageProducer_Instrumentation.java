/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms11;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

@Weave(type = MatchType.Interface, originalName = "javax.jms.MessageProducer")
public abstract class MessageProducer_Instrumentation {

    public abstract Destination getDestination() throws JMSException;

    @Trace(leaf = true)
    public void send(Message message) throws JMSException {
        JmsMetricUtil.processSendMessage(message, getDestination(), AgentBridge.getAgent().getTracedMethod());
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        JmsMetricUtil.processSendMessage(message, getDestination(), AgentBridge.getAgent().getTracedMethod());
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void send(Destination destination, Message message) throws JMSException {
        JmsMetricUtil.processSendMessage(message, destination, AgentBridge.getAgent().getTracedMethod());
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive)
            throws JMSException {
        JmsMetricUtil.processSendMessage(message, destination, AgentBridge.getAgent().getTracedMethod());
        Weaver.callOriginal();
    }
}
