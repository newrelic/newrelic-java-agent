/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.servlet60.NRRequestWrapper;
import com.nr.instrumentation.servlet60.NRResponseWrapper;
import com.nr.instrumentation.servlet60.ServletHelper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Weave(type = MatchType.Interface, originalName = "jakarta.servlet.Filter")
public abstract class Filter_Instrumentation {

    @NewField
    private FilterConfig filterConfig;

    @Trace(dispatcher = true)
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

        NewRelic.getAgent().getTracedMethod().setMetricName("Filter", getClass().getName(), "doFilter");

        // set the request and response if it is not already set
        if (!AgentBridge.getAgent().getTransaction().isWebRequestSet()) {
            AgentBridge.getAgent().getTransaction().setWebRequest(new NRRequestWrapper((HttpServletRequest) request));
        }

        if (!AgentBridge.getAgent().getTransaction().isWebResponseSet()) {
            AgentBridge.getAgent().getTransaction().setWebResponse(
                    new NRResponseWrapper((HttpServletResponse) response));
        }

        FilterConfig config = filterConfig;
        if (config != null) {
            ServletHelper.setAppName(config);
            ServletHelper.setTransactionName(config, (Filter) this);
        }

        Weaver.callOriginal();
    }

    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;

        if (filterConfig.getServletContext() != null) {
            AgentBridge.privateApi.setServerInfo(filterConfig.getServletContext().getServerInfo());
        }

        Weaver.callOriginal();
    }

    /*
     * The FilterConfig must be nulled out here in order to avoid a memory leak. The memory leak is possible when the
     * application containing this filter is reloaded in the application sever.
     * 
     * The @NewField annotation causes the FilterConfig to be stored in a map where the key is the instance of this
     * Filter and the value is a special New Relic object containing the FilterConfig. The map uses weak keys and strong
     * values. A memory leak occurs because the Map contains a strong reference to the FilterConfig which contains a
     * strong reference to the Filter (indirectly). Therefore the Filter does not get gced.
     */
    public void destroy() {
        Weaver.callOriginal();
        filterConfig = null;
    }
}
