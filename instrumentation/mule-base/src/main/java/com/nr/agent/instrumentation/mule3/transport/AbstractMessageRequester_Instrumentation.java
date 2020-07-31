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
import org.mule.api.MuleMessage;

/**
 * The Message Requester is used to explicitly request messages from a message channel or
 * resource rather than subscribing to inbound events or polling for messages.
 * This is often used programatically but will not be used for inbound endpoints
 * configured on services.
 */
@Weave(type = MatchType.BaseClass, originalName = "org.mule.transport.AbstractMessageRequester")
public abstract class AbstractMessageRequester_Instrumentation {

    @Trace(excludeFromTransactionTrace = true)
    protected MuleMessage doRequest(long timeout) {
        return Weaver.callOriginal();
    }

    @Trace(excludeFromTransactionTrace = true)
    public final MuleMessage request(long timeout) {
        return Weaver.callOriginal();
    }

}
