/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.TracedMethod;
import com.nr.agent.instrumentation.utils.header.W3CTraceParentHeader;
import com.nr.agent.instrumentation.utils.span.AttributeMapper;
import com.nr.agent.instrumentation.utils.span.AttributeType;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.internal.AttributeUtil;
import io.opentelemetry.sdk.trace.data.LinkData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.nr.agent.instrumentation.utils.header.HeaderType.NEWRELIC;
import static com.nr.agent.instrumentation.utils.header.HeaderType.W3C_TRACEPARENT;
import static com.nr.agent.instrumentation.utils.header.HeaderType.W3C_TRACESTATE;

/**
 * New Relic Java agent implementation of an OpenTelemetry SpanBuilder,
 * which is used to construct Span instances. Instead of starting an OpenTelemetry
 * Span, this implementation will create a New Relic Java agent Tracer to time
 * the executing code and will potentially start a New Relic Java agent Transaction
 * based on the detected SpanKind type.
 */
class NRSpanBuilder implements SpanBuilder {
    private static final Span NO_OP_SPAN = OpenTelemetry.noop().getTracer("").spanBuilder("").startSpan();
    private final Instrumentation instrumentation;
    private final String spanName;
    private final Map<String, Object> attributes = new HashMap<>();
    private final TracerSharedState sharedState;
    private final Consumer<ExitTracerSpan> endHandler;
    private final InstrumentationLibraryInfo instrumentationLibraryInfo;
    private SpanKind spanKind = SpanKind.INTERNAL;
    private SpanContext parentSpanContext;
    private AttributeMapper attributeMapper = AttributeMapper.getInstance();

    private List<LinkData> links;
    private int totalNumberOfLinksAdded;
    private long startEpochNanos = 0L;

    public NRSpanBuilder(Instrumentation instrumentation, String instrumentationScopeName, String instrumentationScopeVersion, TracerSharedState sharedState,
            String spanName) {
        this.instrumentation = instrumentation;
        this.spanName = spanName;
        this.sharedState = sharedState;
        instrumentationLibraryInfo = InstrumentationLibraryInfo.create(instrumentationScopeName, instrumentationScopeVersion);
        attributes.put(ExitTracerSpan.OTEL_SCOPE_NAME.getKey(), instrumentationScopeName);
        attributes.put(ExitTracerSpan.OTEL_LIBRARY_NAME.getKey(), instrumentationScopeName);
        if (instrumentationScopeVersion != null) {
            attributes.put(ExitTracerSpan.OTEL_SCOPE_VERSION, instrumentationScopeVersion);
            attributes.put(ExitTracerSpan.OTEL_LIBRARY_VERSION, instrumentationScopeVersion);
        }
        if (sharedState.getActiveSpanProcessor().isEndRequired()) {
            endHandler = sharedState.getActiveSpanProcessor()::onEnd;
        } else {
            endHandler = span -> {
            };
        }
        this.totalNumberOfLinksAdded = 0;
        this.startEpochNanos = this.startEpochNanos == 0 ? this.sharedState.getClock().now() : this.startEpochNanos;
    }

    @Override
    public SpanBuilder setParent(Context context) {
        parentSpanContext = Span.fromContext(context).getSpanContext();
        return this;
    }

    @Override
    public SpanBuilder setNoParent() {
        return this;
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext) {
        if (spanContext != null && spanContext.isValid()) {
            this.addLink(LinkData.create(spanContext));
            return this;
        } else {
            return this;
        }
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext, Attributes attributes) {
        if (spanContext != null && spanContext.isValid()) {
            if (attributes == null) {
                attributes = Attributes.empty();
            }

            int totalAttributeCount = attributes.size();
//            this.addLink(LinkData.create(spanContext, AttributeUtil.applyAttributesLimit(attributes, this.spanLimits.getMaxNumberOfAttributesPerLink(), this.spanLimits.getMaxAttributeValueLength()), totalAttributeCount)); // FIXME limits?
            this.addLink(LinkData.create(spanContext, AttributeUtil.applyAttributesLimit(attributes, 254, 255), totalAttributeCount)); // FIXME limits?
            return this;
        } else {
            return this;
        }
    }

    private void addLink(LinkData link) {
        ++this.totalNumberOfLinksAdded;
        if (this.links == null) {
//            this.links = new ArrayList(this.spanLimits.getMaxNumberOfLinks()); // FIXME limits?
            this.links = new ArrayList<>(1000); // FIXME limits?
        }

//        if (this.links.size() != this.spanLimits.getMaxNumberOfLinks()) { // FIXME limits?
        if (this.links.size() != 1000) { // FIXME limits?
            this.links.add(link);
        }
    }

    @Override
    public SpanBuilder setAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, long value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, double value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, boolean value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public <T> SpanBuilder setAttribute(AttributeKey<T> key, T value) {
        attributes.put(key.getKey(), value);
        return this;
    }

    @Override
    public SpanBuilder setSpanKind(SpanKind spanKind) {
        this.spanKind = spanKind;
        return this;
    }

    @Override
    public SpanBuilder setStartTimestamp(long startTimestamp, TimeUnit unit) {
        if (startTimestamp >= 0L && unit != null) {
            this.startEpochNanos = unit.toNanos(startTimestamp);
            return this;
        } else {
            return this;
        }
    }

    /**
     * Called when starting an OpenTelemetry Span and will result in a New Relic
     * Java agent Tracer being created for each OpenTelemetry Span. Depending on
     * the SpanKind type, this method may start a New Relic Java agent Transaction.
     *
     * @return OpenTelemetry Span
     */
    @Override
    public Span startSpan() {
        SpanContext parentSpanContext = this.parentSpanContext == null ?
                Span.fromContext(Context.current()).getSpanContext() : this.parentSpanContext;
        if (SpanKind.SERVER == spanKind) {
            return startServerSpan(parentSpanContext);
        }
        final boolean dispatcher = SpanKind.CONSUMER.equals(spanKind);
        if (dispatcher) {
            AgentBridge.getAgent().getTransaction(true);
        }
        final ExitTracer tracer = instrumentation.createTracer(spanName, getTracerFlags(dispatcher));
        if (tracer == null) {
            return NO_OP_SPAN;
        }
        if (SpanKind.INTERNAL != spanKind) {
            tracer.addCustomAttribute("span.kind", spanKind.name());
        }
        List<LinkData> immutableLinks = this.links == null ? Collections.emptyList() : Collections.unmodifiableList(this.links);
        // TODO REVIEW - we're not picking up the global resources
        return onStart(new ExitTracerSpan(tracer, instrumentationLibraryInfo, spanKind, spanName, parentSpanContext, sharedState.getResource(), attributes,
                endHandler, immutableLinks, totalNumberOfLinksAdded));
    }

    private Span startServerSpan(SpanContext parentSpanContext) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(true);
        final ExtendedRequest request = new ExtendedRequest() {

            @Override
            public String getRequestURI() {
                Object httpRoute = attributes.get(attributeMapper.findProperOtelKey(SpanKind.SERVER, AttributeType.Route, attributes.keySet()));
                return httpRoute == null ? null : httpRoute.toString();
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public Enumeration getParameterNames() {
                return Collections.emptyEnumeration();
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
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }

            @Override
            public String getHeader(String name) {
                if ("User-Agent".equalsIgnoreCase(name)) {
                    return (String) attributes.get(attributeMapper.findProperOtelKey(SpanKind.SERVER, AttributeType.Host, attributes.keySet()));
                }
                // TODO is it possible to get the newrelic DT header from OTel???
                if (NEWRELIC.equalsIgnoreCase(name)) {
                    return null;
                }
                return null;
            }

            @Override
            public List<String> getHeaders(String name) {
                if (name.isEmpty()) {
                    return Collections.emptyList();
                }
                List<String> headers = new ArrayList<>();

                if (W3C_TRACESTATE.equalsIgnoreCase(name)) {
                    Map<String, String> traceState = parentSpanContext.getTraceState().asMap();
                    StringBuilder tracestateStringBuilder = new StringBuilder();
                    // Build full tracestate header incase there are multiple vendors
                    for (Map.Entry<String, String> entry : traceState.entrySet()) {
                        if (tracestateStringBuilder.length() == 0) {
                            tracestateStringBuilder.append(entry.toString());
                        } else {
                            tracestateStringBuilder.append(",").append(entry.toString());
                        }
                    }
                    headers.add(tracestateStringBuilder.toString());
                    return headers;
                }
                if (W3C_TRACEPARENT.equalsIgnoreCase(name)) {
                    String traceParent = W3CTraceParentHeader.create(parentSpanContext);
                    if (!traceParent.isEmpty()) {
                        headers.add(traceParent);
                        return headers;
                    }
                }
                return headers;
            }

            @Override
            public String getMethod() {
                return (String) attributes.get(attributeMapper.findProperOtelKey(SpanKind.SERVER, AttributeType.Method, attributes.keySet()));
            }
        };

        final ExtendedResponse response = new ExtendedResponse() {

            @Override
            public int getStatus() throws Exception {
                Object statusCode = attributes.get(attributeMapper.findProperOtelKey(SpanKind.SERVER, AttributeType.StatusCode, attributes.keySet()));
                return statusCode instanceof Number ? ((Number) statusCode).intValue() : 0;
            }

            @Override
            public String getStatusMessage() throws Exception {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }

            @Override
            public void setHeader(String name, String value) {

            }

            @Override
            public long getContentLength() {
                return 0;
            }
        };
        transaction.requestInitialized(request, response);
        TracedMethod tracedMethod = transaction.getTracedMethod();
        List<LinkData> immutableLinks = this.links == null ? Collections.emptyList() : Collections.unmodifiableList(this.links);
        return onStart(new ExitTracerSpan((ExitTracer) tracedMethod, instrumentationLibraryInfo, spanKind, spanName,
                parentSpanContext, sharedState.getResource(), attributes, endHandler, immutableLinks, totalNumberOfLinksAdded));
    }

    Span onStart(ReadWriteSpan span) {
        // FIXME
        Context parent = Context.current();
        if (sharedState.getActiveSpanProcessor().isStartRequired()) {
            sharedState.getActiveSpanProcessor().onStart(parent, span);
        }
        return span;
    }

    static int getTracerFlags(boolean dispatcher) {
        int flags = TracerFlags.GENERATE_SCOPED_METRIC
                | TracerFlags.TRANSACTION_TRACER_SEGMENT
                | TracerFlags.CUSTOM;
        if (dispatcher) {
            flags |= TracerFlags.DISPATCHER;
        }
        return flags;
    }
}
