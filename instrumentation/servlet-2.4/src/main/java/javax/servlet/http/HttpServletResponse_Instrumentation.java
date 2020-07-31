/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package javax.servlet.http;

import javax.servlet.ServletResponse;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "javax.servlet.http.HttpServletResponse")
public abstract class HttpServletResponse_Instrumentation implements ServletResponse {

    public abstract void setHeader(String name, String value);

    public void setStatus(int sc) {
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatus(sc);
        Weaver.callOriginal();
    }

    public void setStatus(int sc, String sm) {
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatus(sc);
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatusMessage(sm);
        Weaver.callOriginal();
    }

    public void sendError(int sc, String msg) {
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatus(sc);
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatusMessage(msg);
        Weaver.callOriginal();
    }

    public void sendError(int sc) {
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatus(sc);
        Weaver.callOriginal();
    }
}
