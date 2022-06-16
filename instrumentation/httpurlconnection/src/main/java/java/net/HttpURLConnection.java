/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.net;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpurlconnection.MetricState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Weave(type = MatchType.BaseClass)
public abstract class HttpURLConnection extends URLConnection {

    @NewField
    private MetricState metricStateField;

    private MetricState lazyGetMetricState() {
        if (metricStateField == null) {
            metricStateField = new MetricState();
        }
        return metricStateField;
    }

    protected HttpURLConnection(URL url) {
        super(url);
    }

    public abstract URL getURL();

    @Trace(leaf = true)
    public void connect() throws IOException {
        lazyGetMetricState().nonNetworkPreamble(connected, this, "connect");
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public synchronized OutputStream getOutputStream() throws IOException {
        lazyGetMetricState().nonNetworkPreamble(connected, this, "getOutputStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public synchronized InputStream getInputStream() throws IOException {
        MetricState metricState = lazyGetMetricState();
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        metricState.getInputStreamPreamble(connected, this, method);

        InputStream inputStream;
        try {
            // This does a network request (if getResponseCode() wasn't called first)
            inputStream = Weaver.callOriginal();
        } catch (Exception e) {
            // This is the default legacy behavior of the AbstractExternalComponentTracer
            if (e instanceof UnknownHostException) {
                method.setMetricName("External", "UnknownHost", "HttpURLConnection");
            }
            throw e;
        }

        metricState.getInboundPostamble(this, 0, null, "getInputStream", method);
        return inputStream;
    }

    @Trace(leaf = true)
    public int getResponseCode() throws Exception {
        MetricState metricState = lazyGetMetricState();
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        metricState.getResponseCodePreamble(this, method);

        int responseCodeValue;
        try {
            // This does a network request (if getInputStream() wasn't called first)
            responseCodeValue = Weaver.callOriginal();
        } catch (Exception e) {
            // This is the default legacy behavior of the AbstractExternalComponentTracer
            if (e instanceof UnknownHostException) {
                method.setMetricName("External", "UnknownHost", "HttpURLConnection");
            }
            throw e;
        }

        metricState.getInboundPostamble(this, responseCodeValue, null, "getResponseCode", method);
        return responseCodeValue;
    }

    public abstract String getHeaderField(String name);

    public abstract void setRequestProperty(String key, String value);

}
