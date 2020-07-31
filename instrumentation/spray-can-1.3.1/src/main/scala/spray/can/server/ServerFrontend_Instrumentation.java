/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.can.server;

import com.agent.instrumentation.spray.can.RequestWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import spray.http.HttpRequest;

@Weave(originalName = "spray.can.server.ServerFrontend$$anon$2$$anon$1")
public class ServerFrontend_Instrumentation {
    @Trace(dispatcher = true)
    public void spray$can$server$ServerFrontend$$anon$$anon$$openNewRequest(final HttpRequest request,
            final boolean closeAfterResponseCompletion, final RequestState state) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SprayCan");
        AgentBridge.getAgent().getTransaction().setWebRequest(new RequestWrapper(request));
        Weaver.callOriginal();
    }
}
