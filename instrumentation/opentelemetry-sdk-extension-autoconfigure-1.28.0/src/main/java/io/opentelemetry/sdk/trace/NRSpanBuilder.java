package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.tracers.TracerFlags;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class NRSpanBuilder implements SpanBuilder {
    private final Instrumentation instrumentation;
    private final String spanName;
    private final Map<String, Object> attributes = new HashMap<>();
    private SpanKind spanKind= SpanKind.INTERNAL;

    public NRSpanBuilder(Instrumentation instrumentation, String instrumentationScopeName, String instrumentationScopeVersion, String spanName) {
        this.instrumentation = instrumentation;
        this.spanName = spanName;
        attributes.put(ExitTracerSpan.OTEL_LIBRARY_NAME.getKey(), instrumentationScopeName);
        attributes.put(ExitTracerSpan.OTEL_LIBRARY_VERSION, instrumentationScopeVersion);
    }

    @Override
    public SpanBuilder setParent(Context context) {
        return this;
    }

    @Override
    public SpanBuilder setNoParent() {
        return this;
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext) {
        return this;
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext, Attributes attributes) {
        return this;
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
        return this;
    }

    @Override
    public Span startSpan() {
        final ExitTracer tracer = instrumentation.createTracer("Span/" + spanName,
                TracerFlags.GENERATE_SCOPED_METRIC
                        | TracerFlags.TRANSACTION_TRACER_SEGMENT
                        | TracerFlags.CUSTOM);
        if (tracer == null) {
            return NO_OP_SPAN;
        }
        tracer.addCustomAttribute("span.kind", spanKind.name());
        return new ExitTracerSpan(tracer, spanKind, attributes);
    }

    private static final Span NO_OP_SPAN = new Span() {
        @Override
        public <T> Span setAttribute(AttributeKey<T> key, T value) {
            return this;
        }

        @Override
        public Span addEvent(String name, Attributes attributes) {
            return this;
        }

        @Override
        public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
            return this;
        }

        @Override
        public Span setStatus(StatusCode statusCode, String description) {
            return this;
        }

        @Override
        public Span recordException(Throwable exception, Attributes additionalAttributes) {
            return this;
        }

        @Override
        public Span updateName(String name) {
            return this;
        }

        @Override
        public void end() {
        }

        @Override
        public void end(long timestamp, TimeUnit unit) {

        }

        @Override
        public SpanContext getSpanContext() {
            return SpanContext.getInvalid();
        }

        @Override
        public boolean isRecording() {
            return false;
        }
    };
}
