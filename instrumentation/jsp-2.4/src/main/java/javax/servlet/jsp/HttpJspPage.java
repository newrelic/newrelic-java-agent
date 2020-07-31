/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package javax.servlet.jsp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jsp24.JspUtils;

@Weave(type = MatchType.Interface)
public class HttpJspPage {

    @Trace
    public void _jspService(HttpServletRequest request, HttpServletResponse response) {

        JspUtils.setTransactionName(getClass(), AgentBridge.getAgent().getTracedMethod());

        Weaver.callOriginal();

        Object exception = request.getAttribute("javax.servlet.jsp.jspException");
        if (exception instanceof Throwable) {
            AgentBridge.privateApi.reportException((Throwable) exception);
        }
    }
}
