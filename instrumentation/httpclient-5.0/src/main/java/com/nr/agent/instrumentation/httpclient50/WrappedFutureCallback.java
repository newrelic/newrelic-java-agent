/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.api.agent.Trace;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;

import java.net.URISyntaxException;

public class WrappedFutureCallback<T> implements FutureCallback<T> {

    HttpRequest request;
    FutureCallback origCallback;

    public WrappedFutureCallback (HttpRequest request, FutureCallback origCallback) {
        this.request = request;
        this.origCallback = origCallback;
    }

    @Override
    @Trace(async = true)
    public void completed(T response) {
        try {
            InstrumentationUtils.processResponse(request.getUri(), (BasicHttpResponse)response);
        } catch (URISyntaxException e) {
            // TODO throw new IOException(e);
            // TODO can this happen now?  perhaps in one of the overload methods?
        }
        if (origCallback != null) origCallback.completed(response);
    }

    @Override
    @Trace(async = true)
    public void failed(Exception ex) {
        InstrumentationUtils.handleUnknownHost(ex);
        if (origCallback != null) origCallback.failed(ex);
    }

    @Override
    @Trace(async = true)
    public void cancelled() {
        // TODO handle cancellation  anything to do?
        if (origCallback != null) origCallback.cancelled();
    }

}
