/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.transport;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;

/**
 * Abstract implementation of an outbound channel adaptors. Outbound channel adaptors send messages over
 * a specific transport. Different implementations may support different Message Exchange Patterns.
 */
@Weave(type = MatchType.BaseClass, originalName = "org.mule.transport.AbstractMessageDispatcher")
public abstract class AbstractMessageDispatcher_Instrumentation {

    @Trace
    protected void doDispatch(MuleEvent event) {
        Weaver.callOriginal();
    }

    @Trace
    protected MuleMessage doSend(MuleEvent event) {
        return Weaver.callOriginal();
    }

}
