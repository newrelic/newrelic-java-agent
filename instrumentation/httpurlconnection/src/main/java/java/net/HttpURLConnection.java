/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.net;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpurlconnection.MetricState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.nr.agent.instrumentation.httpurlconnection.MetricState.CONNECT_OP;
import static com.nr.agent.instrumentation.httpurlconnection.MetricState.GET_INPUT_STREAM_OP;
import static com.nr.agent.instrumentation.httpurlconnection.MetricState.GET_OUTPUT_STREAM_OP;
import static com.nr.agent.instrumentation.httpurlconnection.MetricState.GET_RESPONSE_CODE_OP;

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

    // connect can be called explicitly, but it doesn't need to be as it will implicitly be called by other
    // methods that read or write over the connection such as getInputStream, getResponseCode, and getOutputStream.
    // Calling connect directly doesn't cause a request to happen over the wire.
    public void connect() throws IOException {
        lazyGetMetricState().nonNetworkPreamble(connected, this, CONNECT_OP);
        Weaver.callOriginal();
    }

    // This can be called to write data over the wire in a fire and forget manner without inspecting the response. There's
    // no guarantee or requirement that another method (e.g. getInputStream) will be called to get the results or response code.
    // Calling this alone should be considered as a valid external call.
    public synchronized OutputStream getOutputStream() throws IOException {
        lazyGetMetricState().nonNetworkPreamble(connected, this, GET_OUTPUT_STREAM_OP);
        return Weaver.callOriginal();
    }

    // getInputStream opens a stream with the intention of reading response data from the server.
    // Calling getInputStream causes a request to happen over the wire.
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

        metricState.getInboundPostamble(this, 0, null, GET_INPUT_STREAM_OP, method);
        return inputStream;
    }

    // getResponseCode gets the status code from an HTTP response message.
    // If the request was already made (e.g. because getInputStream was called before it) it will simply return the status code from the response.
    // Otherwise, it will initiate the request itself by calling getInputStream which calls connect (or potentially getOutputStream if streaming).
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

        metricState.getInboundPostamble(this, responseCodeValue, null, GET_RESPONSE_CODE_OP, method);
        return responseCodeValue;
    }

    public abstract String getHeaderField(String name);

    public abstract void setRequestProperty(String key, String value);

}
