/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.cxf.phase;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Message;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.cxf27.CxfSoapUtils;

@Weave(type = MatchType.BaseClass)
public abstract class AbstractPhaseInterceptor {

    /**
     * Notice faults raised by all Soap Interceptors.
     */
    @Trace
    public void handleFault(Message message) {
        Weaver.callOriginal();
        if (message instanceof SoapMessage) {
            CxfSoapUtils.nameTransaction((SoapMessage) message);
            CxfSoapUtils.handleFault((SoapMessage) message);
            CxfSoapUtils.addCustomAttributes((SoapMessage) message);
        }
    }
}
