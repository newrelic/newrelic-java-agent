/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet.http;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.servlet.ServletResponse;

@Weave(type = MatchType.Interface, originalName = "jakarta.servlet.http.HttpServletResponse")
public abstract class HttpServletResponse_Instrumentation implements ServletResponse {

    public abstract void setHeader(String name, String value);

    public void setStatus(int sc) {
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatus(sc);
        Weaver.callOriginal();
    }

    //    This method was removed in 6.0
    //    public void setStatus(int sc, String sm)

   public void sendError(int sc) {
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatus(sc);
        Weaver.callOriginal();
    }
}
