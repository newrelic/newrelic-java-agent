/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package javax.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.servlet24.NRRequestWrapper;
import com.nr.instrumentation.servlet24.NRResponseWrapper;
import com.nr.instrumentation.servlet24.ServletHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Weave(type = MatchType.Interface, originalName = "javax.servlet.Servlet")
public abstract class Servlet_Instrumentation {

    @Trace(dispatcher = true)
    public void init(ServletConfig config) {
        ServletContext ctx = config.getServletContext();
        if (ctx != null && ctx.getServerInfo() != null) {
            AgentBridge.privateApi.setServerInfo(ctx.getServerInfo());
        }

        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public void service(ServletRequest request, ServletResponse response) {

        NewRelic.getAgent().getTracedMethod().setMetricName("Servlet", getClass().getName(), "service");

        ServletConfig servletConfig = getServletConfig();
        if (servletConfig != null) {
            ServletHelper.setAppName(servletConfig);
            ServletHelper.setTransactionName(servletConfig, (Servlet) this);
        }

        if (!AgentBridge.getAgent().getTransaction().isWebRequestSet()) {
            AgentBridge.getAgent().getTransaction().setWebRequest(new NRRequestWrapper((HttpServletRequest) request));
        }

        if (!AgentBridge.getAgent().getTransaction().isWebResponseSet()) {
            AgentBridge.getAgent().getTransaction().setWebResponse(
                    new NRResponseWrapper((HttpServletResponse) response));
        }

        Weaver.callOriginal();
    }

    public abstract ServletConfig getServletConfig();
}
