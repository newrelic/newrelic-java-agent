/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jettyhttpclient120;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.client.Result;

import java.net.URI;

/**
 * Listener registered on a Jetty 12 {@link Request} before {@code send()} is called.
 * Reports the external HTTP call to New Relic when the response arrives (success or failure)
 * and ends the associated {@link Segment}.
 *
 * <p>Implements {@link Request.FailureListener}, {@link Response.SuccessListener}, and
 * {@link Response.FailureListener} so that every terminal outcome is covered.
 */
public class NRListener
        implements Request.FailureListener, Response.SuccessListener, Response.FailureListener {

    private static final String LIBRARY = "Jetty HttpClient";
    private static final String PROCEDURE = "send";

    private final Segment segment;
    private final URI uri;

    public NRListener(Segment segment, URI uri) {
        this.segment = segment;
        this.uri = uri;
    }

    /** Called when the request itself fails before a response is received. */
    @Override
    public void onFailure(Request request, Throwable failure) {
        if (segment != null) {
            segment.reportAsExternal(
                    HttpParameters.library(LIBRARY)
                            .uri(uri)
                            .procedure(PROCEDURE)
                            .noInboundHeaders()
                            .build());
            segment.end();
        }
    }

    /** Called when the response is received successfully. */
    @Override
    public void onSuccess(Response response) {
        if (segment != null) {
            segment.reportAsExternal(
                    HttpParameters.library(LIBRARY)
                            .uri(uri)
                            .procedure(PROCEDURE)
                            .inboundHeaders(new InboundWrapper(response))
                            .build());
            segment.end();
        }
    }

    /** Called when the response fails (e.g. connection reset, timeout). */
    @Override
    public void onFailure(Response response, Throwable failure) {
        if (segment != null) {
            segment.reportAsExternal(
                    HttpParameters.library(LIBRARY)
                            .uri(uri)
                            .procedure(PROCEDURE)
                            .noInboundHeaders()
                            .build());
            segment.end();
        }
    }
}
