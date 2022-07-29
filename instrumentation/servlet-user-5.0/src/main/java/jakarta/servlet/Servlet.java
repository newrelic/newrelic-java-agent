/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface)
public abstract class Servlet {

    @Trace(dispatcher = true)
    public void service(ServletRequest request, ServletResponse response) {

        if (request instanceof HttpServletRequest) {
            Principal principal = ((HttpServletRequest) request).getUserPrincipal();
            if (principal != null) {
                AgentBridge.getAgent().getTransaction().getAgentAttributes().put("user", principal.getName());
            }
        }

        Weaver.callOriginal();
    }
}
