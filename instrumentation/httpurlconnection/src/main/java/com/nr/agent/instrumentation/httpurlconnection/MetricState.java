/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.external.URISupport;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.OutboundHeaders;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * <p>
 * This class stores the state and methods to add outbound headers and mark a tracer as an external.
 * </p>
 * <p>
 * The methods <em>addOutboundHeadersIfNotAdded</em> and <em>reportExternalCall</em> should only be executed once each.
 * </p>
 * <p>
 * To prevent <em>addOutboundHeadersIfNotAdded</em> from being executed twice, the first call will set the dtTracer variable.
 * <br />
 * dtTracer not being null signals that the method should not be executed again, and the reference is later used to swap guids.
 * </p>
 * <p>
 * The methods <em>inboundPreamble</em> and <em>inboundPostamble</em> are called by the HttpUrlConnection instrumented methods:
 * <em>getInputStream</em>, <em>getResponseCode</em>, <em>getResponseMessage</em>. And these methods make calls to each other.
 * <br/>
 * To prevent a child call from executing <em>inboundPreamble</em> or <em>inboundPostamble</em>, the first call will set the externalTracer.<br/>
 * This signals to <em>inboundPreamble</em> that the method should not be executed and <em>inboundPostamble</em> will only execute if the tracer passed
 * is the same as the one recorded earlier.
 * </p>
 *
 */
public class MetricState {
    private static final String LIBRARY = "HttpURLConnection";
    private static final URI UNKNOWN_HOST = URI.create("UnknownHost");

    // the guids for these tracers are swapped so the DT is attached to the tracer that
    // has the external
    private WeakReference<TracedMethod> dtTracerRef;
    private WeakReference<TracedMethod> externalTracerRef;
    private boolean externalReported = false;

    /**
     * This can be called by either connect or getOutputStream.
     * Even though both these methods start network activity, neither will make a full request.
     * DT/CAT headers have to be added here.
     * Analyzing DTs, only when the other methods are called that we can see a transaction in the downstream entity.
     *
     * @param isConnected whether a connection has already been made
     * @param connection  HttpURLConnection
     */
    public void nonNetworkPreamble(boolean isConnected, HttpURLConnection connection) {

        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        Transaction tx = AgentBridge.getAgent().getTransaction(false);

        if (!isConnected && method.isMetricProducer() && tx != null) {
            addOutboundHeadersIfNotAdded(connection);
        }
    }

    /**
     * Called when any of getInputStream, getResponseCode, getResponseMessage is invoked, before the original code is executed.
     * This code path guarantees that getInboundPostamble will be called by the first method that called this method.
     *
     * @param isConnected true if a connection has already been made, else false
     * @param tracer      traced method that will be the external
     */
    public void inboundPreamble(boolean isConnected, HttpURLConnection connection, TracedMethod tracer) {
        if (externalReported || getExternalTracer() != null) {
            // another method already ran the preamble
            return;
        }
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        setExternalTracer(tracer);

        if (!isConnected && tracer.isMetricProducer() && tx != null) {
            addOutboundHeadersIfNotAdded(connection);
        }
    }

    /**
     * Called when any of getInputStream, getResponseCode, getResponseMessage is invoked, after the original code is executed.
     * This code path is what ultimately calls reportExternalCall to create an External HTTP span.
     * This method will only execute if the tracer passed to this method is the same as the one passed to the first invocation of inboundPreamble.
     *
     * @param operation
     * @param tracer    traced method that will be the external
     */
    public void inboundPostamble(HttpURLConnection connection, int responseCode, String responseMessage, Ops operation,
            TracedMethod tracer) {
        // So the weak reference to external tracer holds an actual tracer instance (if not null) for the duration of this method.
        TracedMethod externalTracer = getExternalTracer();
        // make sure that only the method that first invoked inboundPreamble runs this method
        if (externalReported || externalTracer != tracer) {
            return;
        }
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (tx != null) {
            reportExternalCall(connection, operation, responseCode, responseMessage, externalTracer);
        }
    }

    public void handleException(TracedMethod tracer, Exception e) {
        TracedMethod externalTracer = getExternalTracer();
        if (externalTracer != tracer || externalTracer == null) {
            return;
        }

        if (!externalReported && e instanceof UnknownHostException) {
            externalTracer.reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST)
                    .procedure("failed")
                    .build());
            externalReported = true;
        }

        setDtTracer(null);
        setExternalTracer(null);
    }

    /**
     * Calls the reportAsExternal API. This results in a Span being created for the current TracedMethod/Segment and the Span
     * category being set to http which represents a Span that made an external http request. This is required for external
     * calls to be properly recorded when they are made to a host that isn't another APM entity.
     *
     * @param connection      HttpURLConnection instance
     * @param operation
     * @param responseCode    response code from HttpURLConnection
     * @param responseMessage response message from HttpURLConnection
     */
    void reportExternalCall(HttpURLConnection connection, Ops operation, int responseCode, String responseMessage) {
        reportExternalCall(connection, operation, responseCode, responseMessage, getExternalTracer());
    }

    /**
     * Calls the reportAsExternal API. This results in a Span being created for the current TracedMethod/Segment and the Span
     * category being set to http which represents a Span that made an external http request. This is required for external
     * calls to be properly recorded when they are made to a host that isn't another APM entity.
     *
     * @param connection      HttpURLConnection instance
     * @param operation
     * @param responseCode    response code from HttpURLConnection
     * @param responseMessage response message from HttpURLConnection
     * @param externalTracer  tracer of which the external call will be reported to
     */
    void reportExternalCall(HttpURLConnection connection, Ops operation, int responseCode, String responseMessage, TracedMethod externalTracer) {
        if (connection != null) {
            // This conversion is necessary as it strips query parameters from the URI
            String uri = URISupport.getURI(connection.getURL());
            InboundWrapper inboundWrapper = new InboundWrapper(connection);

            if (externalTracer != null) {
                // This will result in External rollup metrics being generated (e.g. External/all, External/allWeb, External/allOther, External/{HOST}/all)
                // Calling reportAsExternal is what causes an HTTP span to be created
                externalTracer.reportAsExternal(HttpParameters
                        .library(LIBRARY)
                        .uri(URI.create(uri))
                        .procedure(operation.label)
                        .inboundHeaders(inboundWrapper)
                        .status(responseCode, responseMessage)
                        .build());

                // need to call this method to set addedOutboundRequestHeaders in the Tracer
                externalTracer.addOutboundRequestHeaders(DummyHeaders.INSTANCE);
                GuidSwapper.swap(getDtTracer(), externalTracer);
            }

            setDtTracer(null);
            setExternalTracer(null);
            externalReported = true;
        }
    }

    /**
     * Checks whether outboundheaders (DT/CAT) were already added and if not, add them to the connection.
     */
    private void addOutboundHeadersIfNotAdded(HttpURLConnection connection) {
        if (getDtTracer() == null) {
            setDtTracer(AgentBridge.getAgent().getTracedMethod());
            getDtTracer().addOutboundRequestHeaders(new OutboundWrapper(connection));
        }
    }

    private TracedMethod getDtTracer() {
        return dtTracerRef != null ? dtTracerRef.get() : null;
    }

    private void setDtTracer(TracedMethod tracedMethod) {
        dtTracerRef = tracedMethod != null ? new WeakReference<>(tracedMethod) : null;
    }

    private TracedMethod getExternalTracer() {
        return externalTracerRef != null ? externalTracerRef.get() : null;
    }

    private void setExternalTracer(TracedMethod tracedMethod) {
        externalTracerRef = tracedMethod != null ? new WeakReference<>(tracedMethod) : null;
    }

    public enum Ops {
        CONNECT("connect"),
        GET_OUTPUT_STREAM("getOutputStream"),
        GET_INPUT_STREAM("getInputStream"),
        GET_RESPONSE_CODE("getResponseCode"),
        GET_RESPONSE_MSG("getResponseMessage"),
        ;

        Ops(String label) {
            this.label = label;
        }

        private String label;
    }

    private static class DummyHeaders implements OutboundHeaders {
        private static final DummyHeaders INSTANCE = new DummyHeaders();
        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public void setHeader(String name, String value) {

        }
    }
}
