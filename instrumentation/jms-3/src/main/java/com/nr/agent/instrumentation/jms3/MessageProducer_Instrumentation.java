/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

@Weave(type = MatchType.Interface, originalName = "jakarta.jms.MessageProducer")
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
