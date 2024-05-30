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

import static com.nr.agent.instrumentation.httpurlconnection.MetricState.Ops.GET_INPUT_STREAM;
import static com.nr.agent.instrumentation.httpurlconnection.MetricState.Ops.GET_RESPONSE_CODE;
import static com.nr.agent.instrumentation.httpurlconnection.MetricState.Ops.GET_RESPONSE_MSG;

@Weave(type = MatchType.BaseClass)
public abstract class HttpURLConnection extends URLConnection {

    @NewField
    private MetricState metricStateField;

    /**
     * Return the existing MetricState for this object, or create a new one if it does not exist.
     */
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
    @Trace(leaf=true)
    public void connect() throws IOException {
        lazyGetMetricState().nonNetworkPreamble(connected, this);
        Weaver.callOriginal();
    }

    // This can be called to write data over the wire in a fire and forget manner without inspecting the response. There's
    // no guarantee or requirement that another method (e.g. getInputStream) will be called to get the results or response code.
    // Though it is not guaranteed that a call to this method will fire the external call and if it does there is no correlation
    // between this method's call to the start or length of the external call.
    @Trace(leaf=true)
    public synchronized OutputStream getOutputStream() throws IOException {
        lazyGetMetricState().nonNetworkPreamble(connected, this);
        return Weaver.callOriginal();
    }

    // getInputStream opens a stream with the intention of reading response data from the server.
    // Calling getInputStream causes a request to happen over the wire.
    @Trace(leaf=true)
    public synchronized InputStream getInputStream() throws IOException {
        MetricState metricState = lazyGetMetricState();
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        metricState.inboundPreamble(connected, this, method);

        InputStream inputStream;
        try {
            // This does a network request (if getResponseCode() wasn't called first)
            inputStream = Weaver.callOriginal();
        } catch (Exception e) {
            metricState.handleException(method, e);
            throw e;
        }

        metricState.inboundPostamble(this, responseCode, responseMessage, GET_INPUT_STREAM, method);
        return inputStream;
    }

    // getResponseCode gets the status code from an HTTP response message.
    // If the request was already made (e.g. because getInputStream was called before it) it will simply return the status code from the response.
    // Otherwise, it will initiate the request itself by calling getInputStream which calls connect (or potentially getOutputStream if streaming).
    @Trace(leaf=true)
    public int getResponseCode() throws Exception {
        MetricState metricState = lazyGetMetricState();
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        metricState.inboundPreamble(connected, this, method);

        int responseCodeValue;
        try {
            // This does a network request (if getInputStream() wasn't called first)
            responseCodeValue = Weaver.callOriginal();
        } catch (Exception e) {
            metricState.handleException(method, e);
            throw e;
        }

        metricState.inboundPostamble(this, responseCodeValue, responseMessage, GET_RESPONSE_CODE, method);
        return responseCodeValue;
    }

    @Trace(leaf=true)
    public String getResponseMessage()  throws IOException {
        MetricState metricState = lazyGetMetricState();
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        metricState.inboundPreamble(connected, this, method);
        String responseMessageValue;
        try {
            responseMessageValue = Weaver.callOriginal();
        } catch (Exception e) {
            metricState.handleException(method, e);
            throw e;
        }
        metricState.inboundPostamble(this, responseCode, responseMessageValue, GET_RESPONSE_MSG, method);
        return responseMessageValue;
    }

    public abstract String getHeaderField(String name);

    public abstract void setRequestProperty(String key, String value);

    protected int responseCode = Weaver.callOriginal();

    protected String responseMessage = Weaver.callOriginal();
}
