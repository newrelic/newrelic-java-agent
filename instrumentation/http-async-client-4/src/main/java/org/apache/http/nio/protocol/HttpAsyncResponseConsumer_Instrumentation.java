/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.http.nio.protocol;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpasyncclient4.InboundWrapper;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Weaves org.apache.http.nio.protocol.HttpAsyncResponseConsumer.
 */
@Weave(type = MatchType.Interface, originalName = "org.apache.http.nio.protocol.HttpAsyncResponseConsumer")
public class HttpAsyncResponseConsumer_Instrumentation<T> {
    @NewField
    public Segment segment;
    @NewField
    public URI uri;
    @NewField
    private InboundWrapper inboundHeaders;
    @NewField
    private HttpResponse httpResponse;

    /**
     * Invoked when a HTTP response message is received.
     */
    public void responseReceived(HttpResponse response) throws IOException, HttpException {
        inboundHeaders = new InboundWrapper(response);
        httpResponse = response;
        Weaver.callOriginal();
    }

    /**
     * Invoked to signal that the response has been fully processed.
     */
    public void responseCompleted(HttpContext context) {
        if (segment != null && uri != null) {
            segment.reportAsExternal(HttpParameters
                    .library("HttpAsyncClient")
                    .uri(uri)
                    .procedure("responseCompleted")
                    .inboundHeaders(inboundHeaders)
                    .status(getStatusCode(), getReasonMessage())
                    .build());
            segment.end();
        }
        segment = null;
        uri = null;
        inboundHeaders = null;
        Weaver.callOriginal();
    }

    /**
     * Invoked to signal that the response processing terminated abnormally.
     */
    public void failed(Exception ex) {
        if (segment != null) {
            if (ex instanceof UnknownHostException) {
                segment.reportAsExternal(GenericParameters
                        .library("HttpAsyncClient")
                        .uri(URI.create("UnknownHost"))
                        .procedure("failed")
                        .build());
            } else {
                NewRelic.noticeError(ex);
            }
            segment.end();
            segment = null;
            uri = null;
            inboundHeaders = null;
        }
        Weaver.callOriginal();
    }

    private Integer getStatusCode() {
        if (httpResponse != null && httpResponse.getStatusLine() != null) {
            return httpResponse.getStatusLine().getStatusCode();
        }
        return null;
    }

    private String getReasonMessage() {
        if (httpResponse != null && httpResponse.getStatusLine() != null) {
            return httpResponse.getStatusLine().getReasonPhrase();
        }
        return null;
    }
}
