/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Weaves com.ning.http.client.AsyncHandler.  We use the originalName because we need to reference the State enum, so
 * we need this weave class NOT to shadow the original class.
 */
@Weave(type = MatchType.Interface, originalName = "com.ning.http.client.AsyncHandler")
public class NRAsyncHandler<T> {

    /**
     * Ning allows termination of response processing after reading only the status line. After much deliberation, we
     * decided to hold open the connection and read the headers <i>even though the customer requested it be closed</i>
     * so we can pick up CAT information if present. We see very little risk with this approach, though it slightly
     * changes the way the program behaves. We believe not reading the headers will result in support tickets regarding
     * missing CAT information, so we decided to read the headers against the delegate's wishes.
     */
    @NewField
    private AtomicBoolean userAbortedOnStatusReceived;
    @NewField
    public Segment segment;
    @NewField
    public URI uri;
    @NewField
    private InboundWrapper inboundHeaders;
    @NewField
    private HttpResponseStatus responseStatus;

    public AsyncHandler.STATE onStatusReceived(HttpResponseStatus responseStatus) {
        AsyncHandler.STATE userState = Weaver.callOriginal();
        if (userState == AsyncHandler.STATE.ABORT) {
            if (userAbortedOnStatusReceived == null) {
                userAbortedOnStatusReceived = new AtomicBoolean(false);
            }
            userAbortedOnStatusReceived.set(true);
            return AsyncHandler.STATE.CONTINUE;
        }
        this.responseStatus = responseStatus;
        return userState;
    }

    public void onThrowable(Throwable t) {
        if (segment != null) {
            segment.reportAsExternal(GenericParameters
                    .library("AsyncHttpClient")
                    .uri(uri)
                    .procedure("onThrowable")
                    .build());
            // This used to be segment.finish(t), but the agent doesn't automatically report it.
            segment.end();
        }
        responseStatus = null;
        segment = null;
        uri = null;
        inboundHeaders = null;
        userAbortedOnStatusReceived = null;

        Weaver.callOriginal();
    }

    public AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) {
        if (!headers.isTraillingHeadersReceived()) {
            if (segment != null) {
                inboundHeaders = new InboundWrapper(new HashMap<>(headers.getHeaders()));
            }
            if (userAbortedOnStatusReceived != null && userAbortedOnStatusReceived.get()) {
                return AsyncHandler.STATE.ABORT;
            }
        }
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public T onCompleted() throws Exception {
        if (segment != null) {
            // This keeps the transaction alive after "segment.end()" just in case there are any completion handlers
            segment.getTransaction().getToken().linkAndExpire();

            segment.reportAsExternal(HttpParameters
                    .library("AsyncHttpClient")
                    .uri(uri)
                    .procedure("onCompleted")
                    .inboundHeaders(inboundHeaders)
                    .status(getStatusCode(), getReasonMessage())
                    .build());
            //This used to be segment.finish(t), but the agent doesn't automatically report t.
            segment.end();
        }
        responseStatus = null;
        segment = null;
        uri = null;
        inboundHeaders = null;
        userAbortedOnStatusReceived = null;

        return Weaver.callOriginal();
    }

    private Integer getStatusCode() {
        if (responseStatus != null) {
            return responseStatus.getStatusCode();
        }
        return null;
    }

    private String getReasonMessage() {
        if (responseStatus != null) {
            return responseStatus.getStatusText();
        }
        return null;
    }
}