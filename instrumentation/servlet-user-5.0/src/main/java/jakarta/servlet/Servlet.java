/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet;

import java.security.Principal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import jakarta.servlet.http.HttpServletRequest;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface)
public abstract class Servlet {

    @NewField
    private final AtomicBoolean logNrUserDeprecation = new AtomicBoolean(true);

    @Trace(dispatcher = true)
    public void service(ServletRequest request, ServletResponse response) {

        if (request instanceof HttpServletRequest) {
            Principal principal = ((HttpServletRequest) request).getUserPrincipal();
            if (principal != null) {
                // The user attribute is deprecated, the userId is going to be used instead
                AgentBridge.getAgent().getTransaction().getAgentAttributes().put("user", principal.getName());
                if (logNrUserDeprecation.get()) {
                    NewRelic.getAgent().getLogger().log(Level.INFO, "The 'user' attribute for transactions is deprecated, use the 'userId' attribute instead.");
                    logNrUserDeprecation.set(false);
                }
                NewRelic.setUserIdParam(principal.getName());
            }
        }

        Weaver.callOriginal();
    }
}
