/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URLConnection;

import static org.junit.Assert.assertTrue;

/**
 * The purpose of this class is to simulate the woven HttpUrlConnection.
 * Since we can't weave JRE classes in these tests, we can't use the "real" code.
 * This is the best approximation.
 */
public class InstrumentedHttpUrlConnection {
    private MetricState metricState;
    private HttpURLConnection realConnection;

    public InstrumentedHttpUrlConnection(HttpServerRule server) throws URISyntaxException, IOException {
        URLConnection connection = server.getEndPoint().toURL().openConnection();
        assertTrue(connection instanceof HttpURLConnection);
        this.realConnection = (HttpURLConnection) connection;

        this.metricState = new MetricState();
    }

    @Trace(leaf = true)
    public void connect(boolean isConnected) {
        metricState.nonNetworkPreamble(isConnected, realConnection);
    }

    @Trace(leaf = true)
    public void getResponseCode() {
        TracedMethod tracedMethod = AgentBridge.getAgent().getTracedMethod();
        metricState.inboundPreamble(false, realConnection, tracedMethod);
        metricState.inboundPostamble(realConnection, 0, null, MetricState.Ops.GET_RESPONSE_CODE, tracedMethod);
    }

    @Trace(leaf = true)
    public void getInputStream(boolean isConnected) {
        TracedMethod tracedMethod = AgentBridge.getAgent().getTracedMethod();
        metricState.inboundPreamble(isConnected, realConnection, tracedMethod);
        metricState.inboundPostamble(realConnection, 0, null, MetricState.Ops.GET_INPUT_STREAM, tracedMethod);
    }

    @Trace(leaf = true)
    public void getResponseMessage() {
        TracedMethod tracedMethod = AgentBridge.getAgent().getTracedMethod();
        metricState.inboundPreamble(false, realConnection, tracedMethod);
        metricState.inboundPostamble(realConnection, 0, null, MetricState.Ops.GET_RESPONSE_MSG, tracedMethod);
    }

    public HttpURLConnection getRealConnection() {
        return realConnection;
    }
}
