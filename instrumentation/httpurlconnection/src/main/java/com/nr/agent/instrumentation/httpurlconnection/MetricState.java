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
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MetricState {
    private static final String LIBRARY = "HttpURLConnection";
    private static final String CATEGORY = "Java";
    public static final String CONNECT_OP = "connect";
    public static final String GET_OUTPUT_STREAM_OP = "getOutputStream";
    public static final String GET_INPUT_STREAM_OP = "getInputStream";
    public static final String GET_RESPONSE_CODE_OP = "getResponseCode";

    private boolean metricsRecorded;
    private boolean recordedANetworkCall;
    private boolean addedOutboundRequestHeaders;

    // records the last HttpURLConnection operation
    private String lastOperation = null;
    // records true if any methods that cause a network request were called
    private boolean networkRequestMethodCalled;
    // segment used to track timing of external request, add addOutboundRequestHeaders, and reportAsExternal
    private Segment segment;
    private static final String NEW_RELIC_HTTPURLCONNECTION_SEGMENT_CLEANUP = "New Relic HttpURLConnection Segment Cleanup";

    private static final ScheduledThreadPoolExecutor threadPool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(
            HttpURLConnectionConfig.getThreadPoolSize(), new DefaultThreadFactory(NEW_RELIC_HTTPURLCONNECTION_SEGMENT_CLEANUP, true));

    static {
        // This forces cancelled tasks to be immediately removed from the thread pool
        threadPool.setRemoveOnCancelPolicy(true);
    }

    private ScheduledFuture<?> segmentCleanupTaskFuture;

    /**
     * Start Segment timing when the first HttpURLConnection API method is invoked.
     * The Segment timing should end when a request has taken place and an external call has been recorded.
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
     * Keep track of which HttpURLConnection API method was most recently called.
     * If connect or getOutputStream was called first, then start a cleanup task to potentially ignore the segment
     * if it is determined that no network call actually took place.
     *
     * @param operation HttpURLConnection method being invoked
     */
    private void handleSegmentsForNonNetworkMethods(String operation) {
        if (operation.equals(CONNECT_OP) || operation.equals(GET_OUTPUT_STREAM_OP)) {
            // Potentially start a cleanup task if either connect or getOutputStream is the first method called
            if (lastOperation == null) {
                lastOperation = operation;
                /*
                 * Don't start task if operation is getOutputStream and DT is enabled. This is only a necessary precaution when using CAT
                 * as it can't call reportAsExternal (which calls segment.end) in certain conditions when the stream hasn't been read from yet.
                 * When using DT, it will always call reportAsExternal and end the segment properly.
                 */
                if (!(lastOperation.equals(GET_OUTPUT_STREAM_OP) && HttpURLConnectionConfig.distributedTracingEnabled())) {
                    startSegmentCleanupTask();
                }
            }
            if (operation.equals(GET_OUTPUT_STREAM_OP)) {
                // Cancel the SegmentCleanupTask before it runs if possible when DT is enabled and getOutputStream was the last method called
                if (HttpURLConnectionConfig.distributedTracingEnabled()) {
                    if (segmentCleanupTaskFuture != null && !segmentCleanupTaskFuture.isCancelled()) {
                        segmentCleanupTaskFuture.cancel(false);
                    }
                }
                lastOperation = operation;
            }
        } else {
            networkRequestMethodCalled = true;
            // Cancel the SegmentCleanupTask before it runs if possible
            if (segmentCleanupTaskFuture != null && !segmentCleanupTaskFuture.isCancelled()) {
                segmentCleanupTaskFuture.cancel(false);
            }
            lastOperation = operation;
        }
    }

    /**
     * If connect (or getOutputStream with CAT) was the first method invoked from the HttpURLConnection APIs then a
     * cleanup task will be started which will determine if the segment should be ignored or not.
     * <p>
     * Note: If the user configurable segment_timeout is explicitly configured to be lower than the timer delay set
     * here then the segment timing will already have been ended and trying to end/ignore it again will have no effect.
     */
    private void startSegmentCleanupTask() {
        // Submit a SegmentCleanupTask to a ScheduledThreadPoolExecutor to be run after a configured delay
        SegmentCleanupTask segmentCleanupTask = new SegmentCleanupTask(NEW_RELIC_HTTPURLCONNECTION_SEGMENT_CLEANUP);
        segmentCleanupTaskFuture = threadPool.schedule(segmentCleanupTask, HttpURLConnectionConfig.getDelayMs(), TimeUnit.MILLISECONDS);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "HttpURLConnection - number of queued cleanup tasks: " + threadPool.getQueue().size());
    }

    /**
     * A Runnable task that can be scheduled to run to determine if a segment should be ignored or not
     */
    private class SegmentCleanupTask implements Runnable {
        String taskName;

        public SegmentCleanupTask(String taskName) {
            this.taskName = taskName;
        }

        public void run() {
            ignoreNonNetworkSegment();
        }
    }

    /**
     * This method executes when a cleanup task completes. If it is determined that connect (or getOutputStream with CAT) was the first and only HttpURLConnection
     * API called then it will have the effect of ignoring the segment, and no external call will be reported. A supportability metric will also be recorded.
     * The purpose of this is to avoid hitting the default segment timeout of 10 minutes and to also prevent the segment from showing in traces.
     */
    private void ignoreNonNetworkSegment() {
        if (lastOperation != null && segment != null) {
            if (!networkRequestMethodCalled) {
                if (lastOperation.equals(CONNECT_OP) || lastOperation.equals(GET_OUTPUT_STREAM_OP)) {
                    segment.ignore();
                    NewRelic.incrementCounter("Supportability/HttpURLConnection/SegmentIgnore/" + lastOperation);
                }
            }
        }
    }

    /**
     * This can be called when either connect or getOutputStream are invoked.
     * If only connect was called then no external call should be recorded. If getOutputStream was call alone, or in
     * any combination with connect, then an external call should be recorded (except for some scenarios involving CAT).
     *
     * @param isConnected true if a connection has already been made, else false
     * @param connection  HttpURLConnection
     * @param operation   HttpURLConnection method being invoked
     */
    public void nonNetworkPreamble(boolean isConnected, HttpURLConnection connection, String operation) {
        handleSegmentsForNonNetworkMethods(operation);

        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        Transaction tx = AgentBridge.getAgent().getTransaction(false);

        if (!isConnected && method.isMetricProducer() && tx != null) {
            startSegmentIfNull(tx, operation);

            /*
             * Add CAT/Distributed tracing headers to this outbound request.
             *
             * Whichever TracedMethod/Segment calls addOutboundRequestHeaders first will be the method that is associated with making the
             * external request to another APM entity. However, if the external request isn't to another APM entity then this does
             * nothing and reportAsExternal must be called to establish the link between the TracedMethod/Segment and external host.
             */
            if (!addedOutboundRequestHeaders && segment != null) {
                segment.addOutboundRequestHeaders(new OutboundWrapper(connection));
                this.addedOutboundRequestHeaders = true;
            }
        }

        /*
         * Report an external call for getOutputStream only if distributed tracing is being used instead of CAT.
         * If the input stream hasn't been read from already and CAT is being used instead of DT, then calling reportAsExternal will trigger a call to
         * getHeaderField on the HttpURLConnection instance which forces it to connect and read the input stream. Unfortunately, for users of
         * HttpURLConnection this has the unexpected effect of rendering the HttpURLConnection header map immutable as well as causing
         * "ProtocolException: Cannot write output after reading input" and "IOException: Stream is closed" exceptions when attempting to write
         * to the output stream.
         */
        if (lastOperation.equals(GET_OUTPUT_STREAM_OP) && tx != null) {
            if (HttpURLConnectionConfig.distributedTracingEnabled()) {
                networkRequestMethodCalled = true;
                reportExternalCall(connection, lastOperation, 0, null);
            }
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
        handleSegmentsForNonNetworkMethods(GET_INPUT_STREAM_OP);

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
                 * nothing and reportAsExternal must be called to establish the link between the TracedMethod/Segment and external host.
                 */
                if (!addedOutboundRequestHeaders && segment != null) {
                    segment.addOutboundRequestHeaders(new OutboundWrapper(connection));
                    this.addedOutboundRequestHeaders = true;
                }
            }
        }
    }

    /**
     * Called when getResponseCode is invoked.
     * This code path guarantees that getInboundPostamble will ultimately be called and an external call will be reported.
     *
     * @param method traced method that will be the parent of the segment
     */
    public void getResponseCodePreamble(TracedMethod method) {
        handleSegmentsForNonNetworkMethods(GET_RESPONSE_CODE_OP);

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
        handleSegmentsForNonNetworkMethods(operation);

        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && !metricsRecorded && tx != null) {
            startSegmentIfNull(tx, operation);
            this.metricsRecorded = true;

            /*
             * Add CAT/Distributed tracing headers to this outbound request.
             *
             * Whichever TracedMethod/Segment calls addOutboundRequestHeaders first will be the method that is associated with making the
             * external request to another APM entity. However, if the external request isn't to another APM entity then this does
             * nothing and reportAsExternal must be called to establish the link between the TracedMethod/Segment and external host.
             *
             * If already connected then we cannot modify the HttpURLConnection header map and this will fail to add outbound request headers
             */
            if (!addedOutboundRequestHeaders && segment != null) {
                segment.addOutboundRequestHeaders(new OutboundWrapper(connection));
                this.addedOutboundRequestHeaders = true;
            }

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
        if (connection != null && segment != null) {
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
}
