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
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;

@Weave(type = MatchType.Interface)
public abstract class Servlet {

    @Trace(dispatcher = true)
    public void service(ServletRequest request, ServletResponse response) {

        if (request instanceof HttpServletRequest) {
            Principal principal = ((HttpServletRequest) request).getUserPrincipal();
            if (principal != null) {
                if (Boolean.FALSE.equals(NewRelic.getAgent().getConfig().getValue("high_security", false))) {
                    AgentBridge.getAgent().getTransaction().getAgentAttributes().put("user", principal.getName());
                }
                NewRelic.setUserId(principal.getName());
            }
        }

        Weaver.callOriginal();
    }
}
