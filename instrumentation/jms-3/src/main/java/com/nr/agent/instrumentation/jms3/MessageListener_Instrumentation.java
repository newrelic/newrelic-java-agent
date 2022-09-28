/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.jms.Message;

@Weave(type = MatchType.Interface, originalName = "jakarta.jms.MessageListener")
public abstract class MessageListener_Instrumentation {

    @Trace(dispatcher = true)
    public void onMessage(Message message) {
        JmsMetricUtil.processConsume(message, AgentBridge.getAgent().getTracedMethod());
        if (!NewRelic.getAgent().getTransaction().isTransactionNameSet()) {
            // Do not override transaction name unless we started the transaction.
            JmsMetricUtil.nameTransaction(message);
        }
        JmsMetricUtil.saveMessageParameters(message);
        Weaver.callOriginal();
    }
}
