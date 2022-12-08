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
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class MetricState {
    private static final String LIBRARY = "HttpURLConnection";
    private static final String CATEGORY = "Java";
    public static final String CONNECT_OP = "connect";
    public static final String GET_OUTPUT_STREAM_OP = "getOutputStream";
    public static final String GET_INPUT_STREAM_OP = "getInputStream";
    public static final String GET_RESPONSE_CODE_OP = "getResponseCode";

    private boolean metricsRecorded;
    private boolean recordedANetworkCall;

    // records the name of the first method called
    private String firstMethodCalled = null;
    // records true if any methods that cause a network request were called
    private boolean networkRequestMethodCalled;
    // segment used to track timing of external request, add addOutboundRequestHeaders, and reportAsExternal
    private Segment segment;

    /**
     * Start a Segment to capture timing when the first HttpURLConnection method is invoked.
     * The Segment timing should end when a request has taken place and an external call is recorded.
     *
     * @param tx        current Transaction
     * @param operation HttpURLConnection method being invoked
     */
    private void startSegmentIfNull(Transaction tx, String operation) {
        if (segment == null) {
            segment = tx.startSegment(CATEGORY, operation);
            segment.setMetricName(CATEGORY, LIBRARY, operation);
        }
    }

    /**
     * Track which HttpURLConnection method is called first.
     * If it is one of the methods calling nonNetworkPreamble (i.e. connect or getOutputStream), then start a
     * TimerTask to potentially end the timing of the Segment that was started when either of these methods were called.
     *
     * @param connection HttpURLConnection
     * @param operation  HttpURLConnection method being invoked
     */
    private void handleSegmentsForNonNetworkMethods(HttpURLConnection connection, String operation) {
        /*
         * It's possible to call connect and then getOutputStream, in which case we reset firstMethodCalled
         * to getOutputStream so that the timer task properly ends the segment and calls reportAsExternal.
         */
        if (firstMethodCalled == null || (firstMethodCalled.equals(CONNECT_OP) && operation.equals(GET_OUTPUT_STREAM_OP))) {
            firstMethodCalled = operation;
            if (operation.equals(CONNECT_OP) || operation.equals(GET_OUTPUT_STREAM_OP)) {
                startSegmentExpirationTimerTask(connection);
            }
        }
    }

    /**
     * If connect was the first and only method invoked from the HttpURLConnection API
     * within a defined time period then we assume that no request has taken place and
     * quickly end the segment timing to avoid hitting the default segment timeout of 10 minutes.
     * This should be a rare case as there is really no good reason to only call connect and not
     * follow it up with any additional HttpURLConnection API calls, but technically it is possible.
     * <p>
     * If the segment_timeout is manually configured to be lower than the timer delay set here
     * then the segment timing will already have been ended and calling endAsync() again here
     * will have no effect.
     *
     * @param connection HttpURLConnection
     */
    private void startSegmentExpirationTimerTask(HttpURLConnection connection) {
        Timer timer = new Timer("HttpURLConnection Segment Expiration Timer");
        TimerTask task = new TimerTask() {
            public void run() {
                endSegmentForNonNetworkCall(connection);
            }
        };

        long segmentExpirationDelayInMillis = 1000L;
        timer.schedule(task, segmentExpirationDelayInMillis);
    }

    /**
     * This method is called when a TimerTask completes if connect and/or getOutputStream were the only HttpURLConnection APIs called.
     * If only connect is called, the segment is ignored as no external call has been made.
     * If only getOutputStream is called, or any combination of connect and getOutputStream, then an external call is reported and
     * the timing of the segment is ended.
     *
     * @param connection HttpURLConnection
     */
    private void endSegmentForNonNetworkCall(HttpURLConnection connection) {
        if (firstMethodCalled != null && segment != null) {
            if (!networkRequestMethodCalled) {
                // If connect was the first and only method called when the TimerTask completes then simply ignore the segment as no external call was made.
                if (firstMethodCalled.equals(CONNECT_OP)) {
                    segment.ignore();
                } else if (firstMethodCalled.equals(GET_OUTPUT_STREAM_OP)) {
                    // If firstMethodCalled is set to getOutputStream when the TimerTask completes then we invoke reportExternalCall which
                    // will also end the segment timing. In this scenario getOutputStream was called in a fire and forget manner without ever
                    // inspecting the response, which means that the response code and message will both be unavailable on the reported external.
                    reportExternalCall(connection, firstMethodCalled, 0, null);
                }
            }
        }
    }

    /**
     * This can be called when either connect or getOutputStream are invoked.
     * If only connect was called then no external call should be recorded. If getOutputStream was
     * call alone, or in any combination with connect, then an external call should be recorded.
     *
     * @param isConnected true if a connection has already been made, else false
     * @param connection  HttpURLConnection
     * @param operation   HttpURLConnection method being invoked
     */
    public void nonNetworkPreamble(boolean isConnected, HttpURLConnection connection, String operation) {
        handleSegmentsForNonNetworkMethods(connection, operation);

        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        Transaction tx = AgentBridge.getAgent().getTransaction(false);

        if (!isConnected && method.isMetricProducer() && tx != null) {
            startSegmentIfNull(tx, operation);

            /*
             * Add CAT/Distributed tracing headers to this outbound request.
             *
             * Whichever TracedMethod/Segment calls addOutboundRequestHeaders first will be the method that is associated with making the
             * external request to another APM entity. However, if the external request isn't to another APM entity then this does
             * nothing and method.reportAsExternal must be called to establish the link between the TracedMethod/Segment and external host.
             */
            segment.addOutboundRequestHeaders(new OutboundWrapper(connection));
        }
    }

    /**
     * Called when getInputStream is invoked.
     * This code path guarantees that getInboundPostamble will ultimately be called and an external call will be reported.
     *
     * @param isConnected true if a connection has already been made, else false
     * @param connection  HttpURLConnection
     * @param method      traced method that will be the parent of the segment
     */
    public void getInputStreamPreamble(boolean isConnected, HttpURLConnection connection, TracedMethod method) {
        networkRequestMethodCalled = true;
        handleSegmentsForNonNetworkMethods(connection, GET_INPUT_STREAM_OP);

        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && tx != null) {
            startSegmentIfNull(tx, GET_INPUT_STREAM_OP);
            if (!recordedANetworkCall) {
                this.recordedANetworkCall = true;
            }

            if (!isConnected) {

                /*
                 * Add CAT/Distributed tracing headers to this outbound request.
                 *
                 * Whichever TracedMethod/Segment calls addOutboundRequestHeaders first will be the method that is associated with making the
                 * external request to another APM entity. However, if the external request isn't to another APM entity then this does
                 * nothing and method.reportAsExternal must be called to establish the link between the TracedMethod/Segment and external host.
                 */
                segment.addOutboundRequestHeaders(new OutboundWrapper(connection));
            }
        }
    }

    /**
     * Called when getResponseCode is invoked.
     * This code path guarantees that getInboundPostamble will ultimately be called and an external call will be reported.
     *
     * @param connection HttpURLConnection
     * @param method     traced method that will be the parent of the segment
     */
    public void getResponseCodePreamble(HttpURLConnection connection, TracedMethod method) {
        networkRequestMethodCalled = true;
        handleSegmentsForNonNetworkMethods(connection, GET_RESPONSE_CODE_OP);

        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && tx != null && !recordedANetworkCall) {
            this.recordedANetworkCall = true;
            startSegmentIfNull(tx, GET_RESPONSE_CODE_OP);
        }
    }

    /**
     * Called when either getInputStream or getResponseCode is invoked.
     * This code path is what ultimately calls reportExternalCall to create an External HTTP span.
     *
     * @param connection      HttpURLConnection
     * @param responseCode    HttpURLConnection response code
     * @param responseMessage HttpURLConnection response message
     * @param operation       HttpURLConnection method being invoked
     * @param method          traced method that will be the parent of the segment
     */
    public void getInboundPostamble(HttpURLConnection connection, int responseCode, String responseMessage, String operation, TracedMethod method) {
        networkRequestMethodCalled = true;
        handleSegmentsForNonNetworkMethods(connection, operation);

        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && !metricsRecorded && tx != null) {
            startSegmentIfNull(tx, operation);
            this.metricsRecorded = true;

            /*
             * Add CAT/Distributed tracing headers to this outbound request.
             *
             * Whichever TracedMethod/Segment calls addOutboundRequestHeaders first will be the method that is associated with making the
             * external request to another APM entity. However, if the external request isn't to another APM entity then this does
             * nothing and method.reportAsExternal must be called to establish the link between the TracedMethod/Segment and external host.
             *
             * If already connected then we cannot modify the HttpURLConnection header map and this will fail to add outbound request headers
             */
            segment.addOutboundRequestHeaders(new OutboundWrapper(connection));

            // This will result in External rollup metrics being generated
            reportExternalCall(connection, operation, responseCode, responseMessage);
        }
    }

    /**
     * Calls the reportAsExternal API. This results in a Span being created for the current TracedMethod/Segment and the Span
     * category being set to http which represents a Span that made an external http request. This is required for external
     * calls to be properly recorded when they are made to a host that isn't another APM entity.
     *
     * @param connection      HttpURLConnection instance
     * @param operation       HttpURLConnection API method being invoked
     * @param responseCode    response code from HttpURLConnection
     * @param responseMessage response message from HttpURLConnection
     */
    void reportExternalCall(HttpURLConnection connection, String operation, int responseCode, String responseMessage) {
        // This conversion is necessary as it strips query parameters from the URI
        String uri = URISupport.getURI(connection.getURL());
        InboundWrapper inboundWrapper = new InboundWrapper(connection);

        // This will result in External rollup metrics being generated (e.g. External/all, External/allWeb, External/allOther, External/{HOST}/all)
        // Calling reportAsExternal is what causes an HTTP span to be created
        segment.reportAsExternal(HttpParameters
                .library(LIBRARY)
                .uri(URI.create(uri))
                .procedure(operation)
                .inboundHeaders(inboundWrapper)
                .status(responseCode, responseMessage)
                .build());

        segment.end();
    }
}
