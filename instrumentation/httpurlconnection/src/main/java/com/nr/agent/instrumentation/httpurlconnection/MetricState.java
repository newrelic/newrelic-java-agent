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
import com.newrelic.agent.bridge.external.ExternalMetrics;
import com.newrelic.agent.bridge.external.URISupport;
import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.HttpParameters;

import java.net.HttpURLConnection;
import java.net.URI;

public class MetricState {
    private static final String LIBRARY = "HttpURLConnection";
    private boolean metricsRecorded;
    private boolean recordedANetworkCall;

    public void nonNetworkPreamble(boolean isConnected, HttpURLConnection connection, String operation) {
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (!isConnected && method.isMetricProducer() && tx != null) {
            // Add outbound CAT headers
            tx.getCrossProcessState().processOutboundRequestHeaders(new OutboundWrapper(connection), method);
            // This method doesn't have any network I/O so we are explicitly not recording external rollup metrics
            makeNonRollupExternalMetric(connection, method, operation);
            ConcurrentHashMapHeaders headers = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
            tx.insertDistributedTraceHeaders(headers);
        }
    }

    public void getInputStreamPreamble(boolean isConnected, HttpURLConnection connection, TracedMethod method) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && tx != null) {
            if (!recordedANetworkCall) {
                this.recordedANetworkCall = true;
                makeNonRollupExternalMetric(connection, method, "getInputStream");
            }

            if (!isConnected) {
                // Add outbound CAT headers
                tx.getCrossProcessState().processOutboundRequestHeaders(new OutboundWrapper(connection), method);
                ConcurrentHashMapHeaders headers = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
                tx.insertDistributedTraceHeaders(headers);
            }
        }
    }

    public void getResponseCodePreamble(HttpURLConnection connection, TracedMethod method) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && tx != null && !recordedANetworkCall) {
            this.recordedANetworkCall = true;
            makeNonRollupExternalMetric(connection, method, "getResponseCode");
        }
    }

    public void getInboundPostamble(HttpURLConnection connection, int responseCode, String responseMessage, String requestMethod, TracedMethod method) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && !metricsRecorded && tx != null) {
            this.metricsRecorded = true;
            // This conversion is necessary as it strips query parameters from the URI
            String uri = URISupport.getURI(connection.getURL());

            // This will result in External rollup metrics being generated
            method.reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(URI.create(uri))
                    .procedure(requestMethod)
                    .inboundHeaders(new InboundWrapper(connection))
                    .status(responseCode, responseMessage)
                    .build());
        }
    }

    /**
     * Sets external metric name (i.e. External/{HOST}/HttpURLConnection).
     * This does not create rollup metrics such as External/all, External/allWeb, External/allOther, External/{HOST}/all
     *
     * @param connection HttpURLConnection instance
     * @param method     TracedMethod instance
     * @param operation  String representation of operation
     */
    private void makeNonRollupExternalMetric(HttpURLConnection connection, TracedMethod method, String operation) {
        String uri = URISupport.getURI(connection.getURL());
        ExternalMetrics.makeExternalComponentMetric(
                method,
                connection.getURL().getHost(),
                LIBRARY,
                false,
                uri,
                operation);
    }
}
