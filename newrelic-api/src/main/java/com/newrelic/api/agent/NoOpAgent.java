/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Provides NoOps for API objects to avoid returning <code>null</code>. Do not call these objects directly.
 */
class NoOpAgent implements Agent {
    static final Agent INSTANCE = new NoOpAgent();
    private static final TracedMethod TRACED_METHOD = new TracedMethod() {

        @Override
        public void setMetricName(String... metricNameParts) {
        }

        @Override
        public void addCustomAttribute(String key, Number value) {
        }

        @Override
        public void addCustomAttribute(String key, String value) {
        }

        @Override
        public void addCustomAttribute(String key, boolean value) {
        }

        @Override
        public void addCustomAttributes(Map<String, Object> attributes) {
        }

        @Override
        public String getMetricName() {
            return "NoAgent";
        }

        @Override
        public void addRollupMetricName(String... metricNameParts) {
        }

        @Override
        public void reportAsExternal(ExternalParameters externalParameters) {
        }

        @Override
        public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        }
    };

    private static final Transaction TRANSACTION = new Transaction() {

        @Override
        public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category,
                String... parts) {
            return false;
        }

        @Override
        public boolean isTransactionNameSet() {
            return false;
        }

        @Override
        public void ignore() {
        }

        @Override
        public TracedMethod getLastTracer() {
            return getTracedMethod();
        }

        @Override
        public TracedMethod getTracedMethod() {
            return TRACED_METHOD;
        }

        @Override
        public void ignoreApdex() {
        }

        @Override
        public String getRequestMetadata() {
            return null;
        }

        @Override
        public void processRequestMetadata(String metadata) {
        }

        @Override
        public String getResponseMetadata() {
            return null;
        }

        @Override
        public void processResponseMetadata(String metadata) {
        }

        @Override
        public void processResponseMetadata(String responseMetadata, URI uri) {
        }

        @Override
        public void setWebRequest(ExtendedRequest request) {
        }

        @Override
        public void setWebResponse(Response response) {
        }

        @Override
        public boolean markResponseSent() {
            return false;
        }

        @Override
        public boolean isWebTransaction() {
            return false;
        }

        @Override
        public void ignoreErrors() {
        }

        @Override
        public void convertToWebTransaction() {
        }

        @Override
        public void addOutboundResponseHeaders() {
        }

        @Override
        public Token getToken() {
            return TOKEN;
        }

        @Override
        public Segment startSegment(String tracedActivityName) {
            return SEGMENT;
        }

        @Override
        public Segment startSegment(String category, String segmentName) {
            return SEGMENT;
        }

        @Override
        public DistributedTracePayload createDistributedTracePayload() {
            return NO_OP_PAYLOAD;
        }

        @Override
        public void acceptDistributedTracePayload(String payload) {
        }

        @Override
        public void acceptDistributedTracePayload(DistributedTracePayload payload) {
        }

        @Override
        public void insertDistributedTraceHeaders(Headers headers) {
        }

        @Override
        public void acceptDistributedTraceHeaders(TransportType transportType, Headers headers) {
        }

        @Override
        public Object getSecurityMetaData() {
            return new Object();
        }
    };

    private static final Token TOKEN = new Token() {
        @Override
        public boolean expire() {
            return false;
        }

        @Override
        public boolean link() {
            return false;
        }

        @Override
        public boolean linkAndExpire() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    };

    private static final ErrorApi ERROR_API = new ErrorApi() {
        @Override
        public void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
        }

        @Override
        public void noticeError(String message, Map<String, ?> params, boolean expected) {
        }

        @Override
        public void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {
        }
    };

    private static final Logger LOGGER = new Logger() {

        @Override
        public void logToChild(String childName, Level level, String pattern, Object msg1, Object msg2, Object msg3,
                Object msg4) {
        }

        @Override
        public void log(Level level, String pattern) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern) {
        }

        @Override
        public void log(Level level, String pattern, Object[] msg) {
        }

        @Override
        public void log(Level level, String pattern, Object part1) {
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2) {
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3) {
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4) {
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4,
                Object part5) {
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4,
                Object part5, Object part6) {
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4,
                Object part5, Object part6, Object part7) {
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4,
                Object part5, Object part6, Object part7, Object... otherParts) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object[] msg) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object msg1) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object msg1, Object msg2) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object msg1, Object msg2, Object msg3) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object msg1, Object msg2, Object msg3, Object msg4) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object msg1, Object msg2, Object msg3, Object msg4,
                Object msg5) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object msg1, Object msg2, Object msg3, Object msg4,
                Object msg5, Object msg6) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object msg1, Object msg2, Object msg3, Object msg4,
                Object msg5, Object msg6, Object msg7) {
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3,
                Object part4, Object part5, Object part6, Object part7, Object... otherParts) {
        }

        @Override
        public boolean isLoggable(Level level) {
            return false;
        }
    };
    private static final Config CONFIG = new Config() {

        @Override
        public <T> T getValue(String key, T defaultVal) {
            return defaultVal;
        }

        @Override
        public <T> T getValue(String key) {
            return null;
        }
    };
    private static final MetricAggregator METRIC_AGGREGATOR = new MetricAggregator() {

        @Override
        public void recordResponseTimeMetric(String name, long millis) {
        }

        @Override
        public void recordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
        }

        @Override
        public void recordMetric(String name, float value) {
        }

        @Override
        public void incrementCounter(String name, int count) {
        }

        @Override
        public void incrementCounter(String name) {
        }
    };
    private static final Insights INSIGHTS = new Insights() {

        @Override
        public void recordCustomEvent(String eventType, Map<String, ?> attributes) {
        }
    };

    private static final AiMonitoring AI_MONITORING = new AiMonitoring() {
        @Override
        public void recordLlmFeedbackEvent(Map<String, Object> llmFeedbackEventAttributes) {}

        @Override
        public void setLlmTokenCountCallback(LlmTokenCountCallback llmTokenCountCallback) {}
    };

    private static final Segment SEGMENT = new Segment() {
        @Override
        public void setMetricName(String... metricNameParts) {

        }

        @Override
        public void reportAsExternal(ExternalParameters externalParameters) {

        }

        @Override
        public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {

        }

        @Override
        public Transaction getTransaction() {
            return TRANSACTION;
        }

        @Override
        public void ignore() {

        }

        @Override
        public void end() {
        }

        @Override
        public void endAsync() {
        }

        @Override
        public void addCustomAttribute(String key, Number value) {
        }

        @Override
        public void addCustomAttribute(String key, String value) {
        }

        @Override
        public void addCustomAttribute(String key, boolean value) {
        }

        @Override
        public void addCustomAttributes(Map<String, Object> attributes) {
        }
    };

    private static final TraceMetadata TRACE_METADATA = new TraceMetadata() {
        @Override
        public String getTraceId() {
            return "";
        }

        @Override
        public String getSpanId() {
            return "";
        }

        @Override
        public boolean isSampled() {
            return false;
        }
    };

    @Override
    public TracedMethod getTracedMethod() {
        return TRACED_METHOD;
    }

    @Override
    public Transaction getTransaction() {
        return TRANSACTION;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Config getConfig() {
        return CONFIG;
    }

    @Override
    public MetricAggregator getMetricAggregator() {
        return METRIC_AGGREGATOR;
    }

    @Override
    public Insights getInsights() {
        return INSIGHTS;
    }

    @Override
    public AiMonitoring getAiMonitoring() {
        return AI_MONITORING;
    }

    @Override
    public ErrorApi getErrorApi() {
        return ERROR_API;
    }

    @Override
    public TraceMetadata getTraceMetadata() {
        return TRACE_METADATA;
    }

    private static final DistributedTracePayload NO_OP_PAYLOAD = new DistributedTracePayload() {
        @Override
        public String text() {
            return "";
        }

        @Override
        public String httpSafe() {
            return "";
        }
    };

    @Override
    public Map<String, String> getLinkingMetadata() {
        return Collections.emptyMap();
    }
}
