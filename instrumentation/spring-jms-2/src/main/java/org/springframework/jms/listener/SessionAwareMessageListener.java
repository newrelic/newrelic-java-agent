/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.jms.listener;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.springjms2.JmsMetricUtil;

@Weave(type = MatchType.Interface)
public abstract class SessionAwareMessageListener {

    @Trace(dispatcher = true)
    public void onMessage(Message message, Session session) throws JMSException {
        JmsMetricUtil.nameTransaction(message);
        JmsMetricUtil.processConsume(message, AgentBridge.getAgent().getTracedMethod());
        AgentBridge.getAgent().getTransaction().saveMessageParameters(JmsMetricUtil.getMessageParameters(message));
        Weaver.callOriginal();
    }
}
