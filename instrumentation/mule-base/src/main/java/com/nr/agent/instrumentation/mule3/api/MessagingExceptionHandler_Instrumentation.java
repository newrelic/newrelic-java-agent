/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.api;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.mule3.MuleUtils;
import org.mule.api.MuleEvent;

/**
 * Take some action when a messaging exception has occurred (i.e. there was a message in play when the exception occurred).
 */
@Weave(type = MatchType.Interface, originalName = "org.mule.api.exception.MessagingExceptionHandler")
public class MessagingExceptionHandler_Instrumentation {

    @Trace(excludeFromTransactionTrace = true)
    public MuleEvent handleException(Exception exception, MuleEvent event) {
        AgentBridge.privateApi.reportException(exception);
        MuleUtils.handleException(event);
        return Weaver.callOriginal();
    }

}
