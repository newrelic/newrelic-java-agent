/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.jms.listener;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.springjms3.JmsMetricUtil;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

@Weave(type = MatchType.Interface, originalName = "org.springframework.jms.listener.SessionAwareMessageListener" )
public abstract class SessionAwareMessageListener<M extends Message> {

    @Trace(dispatcher = true)
    public void onMessage(M message, Session session) throws JMSException {
        JmsMetricUtil.nameTransaction(message);
        JmsMetricUtil.processConsume(message, AgentBridge.getAgent().getTracedMethod());
        AgentBridge.getAgent().getTransaction().saveMessageParameters(JmsMetricUtil.getMessageParameters(message));
        Weaver.callOriginal();
    }
}
