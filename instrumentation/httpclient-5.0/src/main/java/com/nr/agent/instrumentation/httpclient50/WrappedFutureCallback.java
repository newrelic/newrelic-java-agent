/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Segment;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;

import java.net.URISyntaxException;
import java.util.logging.Level;

public class WrappedFutureCallback<T> implements FutureCallback<T> {

    private HttpRequest request;

    private FutureCallback origCallback;

    private Segment segment;

    public WrappedFutureCallback (HttpRequest request, FutureCallback origCallback, Segment segment) {
        this.request = request;
        this.origCallback = origCallback;
        this.segment = segment;
    }

    @Override
    public void completed(T response) {
        try {
            InstrumentationUtils.processResponse(request.getUri(), (BasicHttpResponse)response, segment);
        } catch (URISyntaxException e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, "Exception with uri: " + e.getMessage());
        }
        if (origCallback != null) origCallback.completed(response);
        if (segment != null) segment.end();
    }

    @Override
    public void failed(Exception ex) {
        InstrumentationUtils.handleUnknownHost(ex);
        if (origCallback != null) origCallback.failed(ex);
        if (segment != null) segment.end();
    }

    @Override
    public void cancelled() {
        if (origCallback != null) origCallback.cancelled();
        if (segment != null) segment.end();
    }

}
