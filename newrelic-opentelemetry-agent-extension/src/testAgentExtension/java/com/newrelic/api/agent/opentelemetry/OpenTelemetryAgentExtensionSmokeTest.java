package com.newrelic.api.agent.opentelemetry;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.TraceMetadata;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import java.util.logging.Level;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class OpenTelemetryAgentExtensionSmokeTest {

    private static final ExtendedRequest DUMMY_REQUEST = new ExtendedRequestImpl();
    private static final ExtendedResponse DUMMY_RESPONSE = new ExtendedResponseImpl();
    private static final Headers DUMMY_HEADERS = new HeadersImpl();
    private static final DistributedTracePayload DUMMY_DISTRIBUTED_TRACE_PAYLOAD = new DistributedTracePayloadImpl();

    @RegisterExtension
    static AgentExtension agentExtension = new AgentExtension();

    /**
     * Invokes the full {@link NewRelic} API surface area.
     *
     * <p>Validates that agent extension has successfully installed by ensuring no
     * exceptions are thrown, and by validating the mock server managed by
     * {@link #agentExtension} receives span, metric, and log data generated from
     * the bridged calls to {@link NewRelic}.
     */
    @Test
    void apiSurfaceArea() throws URISyntaxException, InterruptedException {
        // Agent API
        Agent agent = NewRelic.getAgent();

        // TracedMethod API
        TracedMethod tracedMethod = agent.getTracedMethod();
        tracedMethod.getMetricName();
        tracedMethod.setMetricName("metricNamePart");
        tracedMethod.addRollupMetricName("metricNamePart");
        tracedMethod.reportAsExternal(new ExternalParameters() {
        });
        tracedMethod.addOutboundRequestHeaders(DUMMY_HEADERS);

        // Transaction API
        Transaction transaction = agent.getTransaction();
        transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, true, "category", "parts");
        transaction.isTransactionNameSet();
        transaction.getLastTracer();
        transaction.getTracedMethod();
        transaction.ignore();
        transaction.ignoreApdex();
        transaction.getRequestMetadata();
        transaction.processRequestMetadata("requestMetadata");
        transaction.getResponseMetadata();
        transaction.processResponseMetadata("responseMetadata");
        transaction.processResponseMetadata("responseMetadata", new URI("http://localhost"));
        transaction.setWebRequest(DUMMY_REQUEST);
        transaction.setWebResponse(DUMMY_RESPONSE);
        transaction.markResponseSent();
        transaction.isWebTransaction();
        transaction.ignoreErrors();
        transaction.convertToWebTransaction();
        transaction.addOutboundResponseHeaders();
        Token token = transaction.getToken();
        token.link();
        token.expire();
        token.linkAndExpire();
        token.isActive();
        Segment segment = transaction.startSegment("segment");
        segment.setMetricName("metricNamePart");
        segment.reportAsExternal(new ExternalParameters() {
        });
        segment.addOutboundRequestHeaders(DUMMY_HEADERS);
        segment.getTransaction();
        segment.ignore();
        segment.end();
        segment.endAsync();
        transaction.startSegment("category", "segmentName");
        DistributedTracePayload distributedTracePayload = transaction.createDistributedTracePayload();
        distributedTracePayload.text();
        distributedTracePayload.httpSafe();
        transaction.acceptDistributedTracePayload("payload");
        transaction.acceptDistributedTracePayload(DUMMY_DISTRIBUTED_TRACE_PAYLOAD);
        transaction.insertDistributedTraceHeaders(DUMMY_HEADERS);
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, DUMMY_HEADERS);
        transaction.getSecurityMetaData();

        // Logger API
        Logger logger = agent.getLogger();
        logger.isLoggable(Level.FINE);
        logger.log(Level.FINE, "pattern", new Object[] {});
        logger.log(Level.FINE, "pattern");
        logger.log(Level.FINE, "pattern", new Object());
        logger.log(Level.FINE, "pattern", new Object(), new Object());
        logger.log(Level.FINE, "pattern", new Object(), new Object(), new Object());
        logger.log(Level.FINE, "pattern", new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, "pattern", new Object(), new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, "pattern", new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, "pattern", new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, "pattern", new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, new Throwable(), "pattern", new Object[] {});
        logger.log(Level.FINE, new Throwable(), "pattern");
        logger.log(Level.FINE, new Throwable(), "pattern", new Object());
        logger.log(Level.FINE, new Throwable(), "pattern", new Object(), new Object());
        logger.log(Level.FINE, new Throwable(), "pattern", new Object(), new Object(), new Object());
        logger.log(Level.FINE, new Throwable(), "pattern", new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, new Throwable(), "pattern", new Object(), new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, new Throwable(), "pattern", new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, new Throwable(), "pattern", new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
        logger.log(Level.FINE, new Throwable(), "pattern", new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object(),
                new Object());
        logger.logToChild("child", Level.FINE, "pattern", new Object(), new Object(), new Object(), new Object());

        // Config API
        Config config = agent.getConfig();
        config.getValue("value");
        config.getValue("value", "default");

        // Insights API
        Insights insights = agent.getInsights();
        insights.recordCustomEvent("eventType", Collections.emptyMap());

        // TraceMetadata API
        TraceMetadata traceMetadata = agent.getTraceMetadata();
        traceMetadata.getTraceId();
        traceMetadata.getSpanId();
        traceMetadata.isSampled();

        agent.getLinkingMetadata();
        // End Agent API

        // Metric API
        NewRelic.recordMetric("metric", 1.0f);
        NewRelic.recordResponseTimeMetric("metric", 1);
        NewRelic.incrementCounter("metric");
        NewRelic.incrementCounter("metric", 1);

        // Error API
        NewRelic.noticeError(new Throwable(), Collections.emptyMap());
        NewRelic.noticeError(new Throwable());
        NewRelic.noticeError("message", Collections.emptyMap());
        NewRelic.noticeError(new Throwable(), Collections.emptyMap(), false);
        NewRelic.noticeError(new Throwable(), false);
        NewRelic.noticeError("message", Collections.emptyMap(), false);
        NewRelic.noticeError("message", false);

        // Transaction API
        NewRelic.addCustomParameter("key", 1.0);
        NewRelic.addCustomParameter("key", "value");
        NewRelic.addCustomParameter("key", true);
        NewRelic.addCustomParameters(Collections.emptyMap());
        NewRelic.setUserId("userId");
        NewRelic.setTransactionName("category", "name");
        NewRelic.ignoreTransaction();
        NewRelic.ignoreApdex();
        NewRelic.setRequestAndResponse(DUMMY_REQUEST, DUMMY_RESPONSE);

        // RUM API
        NewRelic.getBrowserTimingHeader();
        NewRelic.getBrowserTimingHeader("nonce");
        NewRelic.getBrowserTimingFooter();
        NewRelic.getBrowserTimingFooter("nonce");
        NewRelic.setUserName("name");
        NewRelic.setAccountName("name");
        NewRelic.setProductName("name");

        // Web Frameworks API
        NewRelic.setAppServerPort(1);
        NewRelic.setServerInfo("dispatcherName", "version");
        NewRelic.setInstanceName("instanceName");
        NewRelic.setErrorGroupCallback(errorData -> "groupingString");

        // Sleep and validate metric, and log data was produced by the calls to the NewRelic API
        // Note, while the NewRelic API bridge adds attributes to spans, it does not create any new ones, so no spans are expected
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertThat(agentExtension.getMetricRequests())
                            .hasSizeGreaterThanOrEqualTo(1)
                            .anySatisfy(metricRequest -> assertThat(metricRequest.getResourceMetricsList().get(0).getScopeMetricsList())
                                    .anySatisfy(scopeMetrics -> assertThat(scopeMetrics.getScope().getName()).isEqualTo("com.newrelic.opentelemetry-bridge")));

                    assertThat(agentExtension.getLogRequests())
                            .hasSizeGreaterThanOrEqualTo(1)
                            .anySatisfy(logRequest -> assertThat(logRequest.getResourceLogsList().get(0).getScopeLogsList())
                                    .anySatisfy(scopeLogs -> assertThat(scopeLogs.getScope().getName()).isEqualTo("com.newrelic.opentelemetry-bridge")));
                });
    }

    private static class HeadersImpl implements Headers {
        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return null;
        }

        @Override
        public void setHeader(String name, String value) {
        }

        @Override
        public void addHeader(String name, String value) {
        }

        @Override
        public Collection<String> getHeaderNames() {
            return null;
        }

        @Override
        public boolean containsHeader(String name) {
            return false;
        }
    }

    private static class ExtendedRequestImpl extends ExtendedRequest {
        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getRequestURI() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public Enumeration getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return new String[0];
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        @Override
        public String getMethod() {
            return null;
        }
    }

    private static class ExtendedResponseImpl extends ExtendedResponse {
        @Override
        public long getContentLength() {
            return 0;
        }

        @Override
        public HeaderType getHeaderType() {
            return null;
        }

        @Override
        public void setHeader(String name, String value) {

        }

        @Override
        public int getStatus() {
            return 0;
        }

        @Override
        public String getStatusMessage() {
            return null;
        }

        @Override
        public String getContentType() {
            return null;
        }
    }

    private static class DistributedTracePayloadImpl implements DistributedTracePayload {
        @Override
        public String text() {
            return null;
        }

        @Override
        public String httpSafe() {
            return null;
        }
    }
}
