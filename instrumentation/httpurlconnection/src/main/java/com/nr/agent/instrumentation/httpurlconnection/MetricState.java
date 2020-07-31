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

import java.net.HttpURLConnection;

public class MetricState {

    private boolean metricsRecorded;
    private boolean recordedANetworkCall;

    public void nonNetworkPreamble(boolean isConnected, HttpURLConnection connection, String operation) {
        TracedMethod method = AgentBridge.getAgent().getTracedMethod();
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (!isConnected && method.isMetricProducer() && tx != null) {
            // This method doesn't have any network I/O so we are explicitly not recording external rollup metrics
            makeMetric(connection, method, operation);
            tx.getCrossProcessState().processOutboundRequestHeaders(new OutboundWrapper(connection), method);
        }
    }

    public void getInputStreamPreamble(boolean isConnected, HttpURLConnection connection, TracedMethod method) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && tx != null) {
            if (!recordedANetworkCall) {
                this.recordedANetworkCall = true;
                makeMetric(connection, method, "getInputStream");
            }

            if (!isConnected) {
                tx.getCrossProcessState().processOutboundRequestHeaders(new OutboundWrapper(connection), method);
            }
        }
    }

    public void getResponseCodePreamble(HttpURLConnection connection, TracedMethod method) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && tx != null && !recordedANetworkCall) {
            this.recordedANetworkCall = true;
            makeMetric(connection, method, "getResponseCode");
        }
    }

    public void getInboundPostamble(HttpURLConnection connection, TracedMethod method) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (method.isMetricProducer() && !metricsRecorded && tx != null) {
            this.metricsRecorded = true;
            String uri = URISupport.getURI(connection.getURL());
            InboundWrapper inboundWrapper = new InboundWrapper(connection);
            tx.getCrossProcessState()
                    .processInboundResponseHeaders(inboundWrapper, method, connection.getURL().getHost(), uri, true);
        }
    }

    private void makeMetric(HttpURLConnection connection, TracedMethod method, String operation) {
        String uri = URISupport.getURI(connection.getURL());
        ExternalMetrics.makeExternalComponentMetric(
                method,
                connection.getURL().getHost(),
                "HttpURLConnection",
                false,
                uri,
                operation);
    }
}
