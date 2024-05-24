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

import javax.jms.JMSException;
import javax.jms.Message;

@Weave(type = MatchType.Interface, originalName = "javax.jms.MessageConsumer")
public abstract class MessageConsumer_Instrumentation {

    @Trace(leaf = true)
    public Message receive() throws JMSException {
        Message message = Weaver.callOriginal();
        JmsMetricUtil.processConsume(message, AgentBridge.getAgent().getTracedMethod());
        return message;
    }

    @Trace(leaf = true)
    public Message receive(long timeout) throws JMSException {
        Message message = Weaver.callOriginal();
        JmsMetricUtil.processConsume(message, AgentBridge.getAgent().getTracedMethod());
        return message;
    }

    @Trace(leaf = true)
    public Message receiveNoWait() throws JMSException {
        Message message = Weaver.callOriginal();
        JmsMetricUtil.processConsume(message, AgentBridge.getAgent().getTracedMethod());
        return message;
    }
}
