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

@Weave(type = MatchType.BaseClass, originalName = "org.mule.transport.AbstractReceiverWorker")
public abstract class AbstractReceiverWorker_Instrumentation {

    /**
     * This is a place where one-way flows can start, like when monitoring a JMS queue.
     */
    @Trace(dispatcher = true)
    public void processMessages() {
        Weaver.callOriginal();
    }

}
