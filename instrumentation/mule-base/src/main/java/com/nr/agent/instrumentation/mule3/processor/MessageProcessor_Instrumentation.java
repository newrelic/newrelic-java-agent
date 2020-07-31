/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.processor;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.mule3.MuleUtils;
import org.mule.api.MuleEvent;

/**
 * Processes MuleEvent's. Implementations that do not mutate the MuleEvent or pass it on to another
 * MessageProcessor should return the MuleEvent they receive.
 */
@Weave(type = MatchType.Interface, originalName = "org.mule.api.processor.MessageProcessor")
public abstract class MessageProcessor_Instrumentation {

    /**
     * Invokes the MessageProcessor.
     */
    @Trace(async = true, excludeFromTransactionTrace = true)
    public MuleEvent process(MuleEvent event) {
        Object tokenKey = event.getFlowVariable(MuleUtils.MULE_EVENT_TOKEN_KEY);
        if (tokenKey != null) {
            MuleUtils.FlowKey key = (MuleUtils.FlowKey) tokenKey;
            Token token = MuleUtils.getToken(key);
            if (token != null) {
                token.link();
            }
        }

        return Weaver.callOriginal();
    }

}
