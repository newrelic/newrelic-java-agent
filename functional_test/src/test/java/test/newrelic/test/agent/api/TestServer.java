/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.api;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Trace;
import fi.iki.elonen.NanoHTTPD;
import org.junit.Assert;

/*
 * Embedded NanoHTTPD server for testing CAT API
 */

public class TestServer extends NanoHTTPD {

    public TestServer(int port) {
        super(port);
    }

    @Override
    @Trace(dispatcher = true)
    public Response serve(IHTTPSession session) {

        Assert.assertNotNull(session.getHeaders().get("x-newrelic-id"));
        Assert.assertNotNull(session.getHeaders().get("x-newrelic-transaction"));
        Response response = newFixedLengthResponse("<html><body><h1>Successful Response</h1>\n</body></html>\n");

        if (!AgentBridge.getAgent().getTransaction().isWebRequestSet()) {
            AgentBridge.getAgent().getTransaction().setWebRequest(new ApiTestHelper.CustomRequestWrapper(session, HeaderType.HTTP));
        }
        if (AgentBridge.getAgent().getTransaction().isWebRequestSet()) {
            AgentBridge.getAgent().getTransaction().setWebResponse(new ApiTestHelper.CustomResponseWrapper(response, HeaderType.HTTP));
        }

        AgentBridge.getAgent().getTransaction().addOutboundResponseHeaders();
        AgentBridge.getAgent().getTransaction().markResponseSent();

        Assert.assertNotNull(response.getHeader("X-NewRelic-App-Data"));
        return response;
    }
}
