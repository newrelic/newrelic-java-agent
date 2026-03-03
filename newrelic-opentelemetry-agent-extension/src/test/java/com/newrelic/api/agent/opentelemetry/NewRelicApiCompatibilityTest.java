/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

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
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.TraceMetadata;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import com.newrelic.opentelemetry.OpenTelemetryAgent;
import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SuppressWarnings("deprecation")
class NewRelicApiCompatibilityTest {

    private static final ExtendedRequest DUMMY_REQUEST = new ExtendedRequestImpl();
    private static final ExtendedResponse DUMMY_RESPONSE = new ExtendedResponseImpl();
    private static final Headers DUMMY_HEADERS = new HeadersImpl();
    private static final DistributedTracePayload DUMMY_DISTRIBUTED_TRACE_PAYLOAD = new DistributedTracePayloadImpl();

    @ParameterizedTest
    @MethodSource("supportedOperationsArgs")
    void supportedOperations(Runnable operation) throws Exception {
        // Setup log capturer for each operation
        LogCapturer logCapturer = LogCapturer.create().captureForLogger(OpenTelemetryNewRelic.class.getName(), Level.TRACE);
        logCapturer.beforeTestExecution(null);
        assertThat(logCapturer.size()).isEqualTo(0);

        // Run operation, and verify no exception and no warning logs
        assertThatCode(operation::run).doesNotThrowAnyException();
        assertThat(logCapturer.getEvents().stream().map(LoggingEvent::getMessage).collect(Collectors.toList())).isEmpty();
    }

    private static Stream<Arguments> supportedOperationsArgs() {
        OpenTelemetryAgent agent = (OpenTelemetryAgent) OpenTelemetryNewRelic.getAgent();
        TracedMethod tracedMethod = agent.getTracedMethod();
        Transaction transaction = agent.getTransaction();
        MetricAggregator metricAggregator = agent.getMetricAggregator();
        Insights insights = agent.getInsights();
        return Stream.of(
                // OpenTelemetryNewRelic
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.getAgent()),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.recordMetric("name", 1.1f)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.recordResponseTimeMetric("name", 1)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.incrementCounter("name")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.incrementCounter("name", 1)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.noticeError(new Throwable(), Collections.emptyMap())),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.noticeError(new Throwable())),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.noticeError("message", Collections.emptyMap())),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.noticeError("message")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.noticeError(new Throwable(), Collections.emptyMap(), false)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.noticeError(new Throwable(), false)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.noticeError("message", Collections.emptyMap(), false)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.noticeError("message", false)),
                // OpenTelemetryAgent
                Arguments.of((Runnable) () -> agent.getTracedMethod()),
                Arguments.of((Runnable) () -> agent.getTransaction()),
                Arguments.of((Runnable) () -> agent.getMetricAggregator()),
                Arguments.of((Runnable) () -> agent.getInsights()),
                // OpenTelemetryTracedMethod
                Arguments.of((Runnable) () -> tracedMethod.addCustomAttribute("key", 1)),
                Arguments.of((Runnable) () -> tracedMethod.addCustomAttribute("key", "value")),
                Arguments.of((Runnable) () -> tracedMethod.addCustomAttribute("key", false)),
                Arguments.of((Runnable) () -> tracedMethod.addCustomAttributes(Collections.emptyMap())),
                // OpenTelemetryTransaction
                Arguments.of((Runnable) () -> transaction.getLastTracer()),
                Arguments.of((Runnable) () -> transaction.getTracedMethod()),
                // OpenTelemetryMetricsAggregator
                Arguments.of((Runnable) () -> metricAggregator.recordResponseTimeMetric("name", 1, 2, TimeUnit.DAYS)),
                Arguments.of((Runnable) () -> metricAggregator.recordMetric("name", 1.1f)),
                Arguments.of((Runnable) () -> metricAggregator.recordResponseTimeMetric("name", 1)),
                Arguments.of((Runnable) () -> metricAggregator.incrementCounter("name")),
                Arguments.of((Runnable) () -> metricAggregator.incrementCounter("name", 1)),
                // OpenTelemetryInsights
                Arguments.of((Runnable) () -> insights.recordCustomEvent("eventType", Collections.emptyMap()))
        );
    }

    @ParameterizedTest
    @MethodSource("unsupportedOperationsArgs")
    void unsupportedOperations(Runnable operation) throws Exception {
        // Setup log capturer for each operation
        LogCapturer logCapturer = LogCapturer.create().captureForLogger(OpenTelemetryNewRelic.class.getName(), Level.TRACE);
        logCapturer.beforeTestExecution(null);
        assertThat(logCapturer.size()).isEqualTo(0);

        // Run operation, and verify no exception and the expected warning log is produced
        assertThatCode(operation::run).doesNotThrowAnyException();
        assertThat(logCapturer.getEvents())
                .hasSize(1)
                .satisfiesExactly(event -> {
                    assertThat(event.getMessage()).matches(
                            "NewRelic API .* was called but is not supported by OpenTelemetryNewRelic bridge\\. Throwable points to code that called method\\.");
                    assertThat(event.getLevel()).isEqualTo(Level.TRACE);
                    assertThat(event.getThrowable()).isNotNull();
                });
    }

    private static Stream<Arguments> unsupportedOperationsArgs() throws Exception {
        URI uri = new URI("http://localhost");
        Agent agent = OpenTelemetryNewRelic.getAgent();
        Logger logger = agent.getLogger();
        Config config = agent.getConfig();
        TracedMethod tracedMethod = agent.getTracedMethod();
        Transaction transaction = agent.getTransaction();
        Token token = transaction.getToken();
        Segment segment = transaction.startSegment("segment");
        DistributedTracePayload distributedTracePayload = transaction.createDistributedTracePayload();
        TraceMetadata traceMetadata = agent.getTraceMetadata();
        return Stream.of(
                // OpenTelemetryNewRelic
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.addCustomParameter("key", 1)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.addCustomParameter("key", "value")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.addCustomParameter("key", false)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.addCustomParameters(Collections.emptyMap())),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setTransactionName("category", "name")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setUserId("userId")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.ignoreTransaction()),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.ignoreApdex()),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setRequestAndResponse(DUMMY_REQUEST, DUMMY_RESPONSE)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.getBrowserTimingHeader()),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.getBrowserTimingHeader("nonce")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setUserName("name")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setAccountName("name")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setProductName("name")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setAppServerPort(1)),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setServerInfo("dispatcherName", "version")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setInstanceName("instanceName")),
                Arguments.of((Runnable) () -> OpenTelemetryNewRelic.setErrorGroupCallback(errorData -> null)),
                // OpenTelemetryAgent
                Arguments.of((Runnable) () -> agent.getLogger()),
                Arguments.of((Runnable) () -> agent.getConfig()),
                Arguments.of((Runnable) () -> agent.getTraceMetadata()),
                Arguments.of((Runnable) () -> agent.getLinkingMetadata()),
                // OpenTelemetryTracedMethod
                Arguments.of((Runnable) () -> tracedMethod.getMetricName()),
                Arguments.of((Runnable) () -> tracedMethod.setMetricName("metricNamePart")),
                Arguments.of((Runnable) () -> tracedMethod.addRollupMetricName("metricNamePart")),
                Arguments.of((Runnable) () -> tracedMethod.reportAsExternal(new ExternalParameters() {
                })),
                Arguments.of((Runnable) () -> tracedMethod.addOutboundRequestHeaders(DUMMY_HEADERS)),
                // OpenTelemetryTransaction
                Arguments.of((Runnable) () -> transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "category", "part")),
                Arguments.of((Runnable) () -> transaction.isTransactionNameSet()),
                Arguments.of((Runnable) () -> transaction.ignore()),
                Arguments.of((Runnable) () -> transaction.ignoreApdex()),
                Arguments.of((Runnable) () -> transaction.getResponseMetadata()),
                Arguments.of((Runnable) () -> transaction.processRequestMetadata("requestMetadata")),
                Arguments.of((Runnable) () -> transaction.getResponseMetadata()),
                Arguments.of((Runnable) () -> transaction.processResponseMetadata("responseMetadata", uri)),
                Arguments.of((Runnable) () -> transaction.setWebRequest(DUMMY_REQUEST)),
                Arguments.of((Runnable) () -> transaction.setWebResponse(DUMMY_RESPONSE)),
                Arguments.of((Runnable) () -> transaction.markResponseSent()),
                Arguments.of((Runnable) () -> transaction.isWebTransaction()),
                Arguments.of((Runnable) () -> transaction.ignoreErrors()),
                Arguments.of((Runnable) () -> transaction.convertToWebTransaction()),
                Arguments.of((Runnable) () -> transaction.addOutboundResponseHeaders()),
                Arguments.of((Runnable) () -> transaction.getToken()),
                Arguments.of((Runnable) () -> transaction.startSegment("segmentName")),
                Arguments.of((Runnable) () -> transaction.startSegment("category", "segmentName")),
                Arguments.of((Runnable) () -> transaction.createDistributedTracePayload()),
                Arguments.of((Runnable) () -> transaction.acceptDistributedTracePayload("payload")),
                Arguments.of((Runnable) () -> transaction.acceptDistributedTracePayload(DUMMY_DISTRIBUTED_TRACE_PAYLOAD)),
                Arguments.of((Runnable) () -> transaction.insertDistributedTraceHeaders(DUMMY_HEADERS)),
                Arguments.of((Runnable) () -> transaction.acceptDistributedTraceHeaders(TransportType.HTTP, DUMMY_HEADERS)),
                Arguments.of((Runnable) () -> transaction.getSecurityMetaData()),
                // NoOpToken
                Arguments.of((Runnable) () -> token.link()),
                Arguments.of((Runnable) () -> token.expire()),
                Arguments.of((Runnable) () -> token.linkAndExpire()),
                Arguments.of((Runnable) () -> token.isActive()),
                // NoOpSegment
                Arguments.of((Runnable) () -> segment.addCustomAttribute("key", 1)),
                Arguments.of((Runnable) () -> segment.addCustomAttribute("key", "value")),
                Arguments.of((Runnable) () -> segment.addCustomAttribute("key", false)),
                Arguments.of((Runnable) () -> segment.addCustomAttributes(Collections.emptyMap())),
                Arguments.of((Runnable) () -> segment.setMetricName("metricNamePart")),
                Arguments.of((Runnable) () -> segment.end()),
                Arguments.of((Runnable) () -> segment.endAsync()),
                // NoOpDistributedTracePayload
                Arguments.of((Runnable) () -> distributedTracePayload.text()),
                Arguments.of((Runnable) () -> distributedTracePayload.httpSafe()),
                // NoOpLogger
                Arguments.of((Runnable) () -> logger.isLoggable(java.util.logging.Level.FINEST)),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object[] {})),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern")),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object(), new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object(), new Object(), new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object(), new Object(), new Object(), new Object(),
                        new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object(), new Object(), new Object(), new Object(),
                        new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object(), new Object(), new Object(), new Object(),
                        new Object(), new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, "pattern", new Object(), new Object(), new Object(), new Object(),
                        new Object(), new Object(), new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object[] {})),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern")),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object(), new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object(), new Object(), new Object(),
                        new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object(), new Object(), new Object(),
                        new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object(), new Object(), new Object(),
                        new Object(), new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object(), new Object(), new Object(),
                        new Object(), new Object(), new Object(), new Object())),
                Arguments.of((Runnable) () -> logger.log(java.util.logging.Level.FINEST, new Throwable(), "pattern", new Object(), new Object(), new Object(),
                        new Object(), new Object(), new Object(), new Object(), new Object())),
                Arguments.of(
                        (Runnable) () -> logger.logToChild("childName", java.util.logging.Level.FINEST, "pattern", new Object(), new Object(), new Object(),
                                new Object())),
                // NoOpConfig
                Arguments.of((Runnable) () -> config.getValue("key")),
                Arguments.of((Runnable) () -> config.getValue("key", "defaultVal")),
                // NoOpTraceMetadata
                Arguments.of((Runnable) () -> traceMetadata.getTraceId()),
                Arguments.of((Runnable) () -> traceMetadata.getSpanId()),
                Arguments.of((Runnable) () -> traceMetadata.isSampled())
        );
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
