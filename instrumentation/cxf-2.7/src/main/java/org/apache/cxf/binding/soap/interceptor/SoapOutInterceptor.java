/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.cxf.binding.soap.interceptor;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.cxf27.CxfSoapUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;

@Weave
public class SoapOutInterceptor {

    /**
     * Instrument soap messages once they're built up but before they're sent out.
     */
    @Trace
    public void handleMessage(SoapMessage message) {
        Weaver.callOriginal();
        CxfSoapUtils.nameTransaction(message);
        CxfSoapUtils.addCustomAttributes(message);
    }

}
