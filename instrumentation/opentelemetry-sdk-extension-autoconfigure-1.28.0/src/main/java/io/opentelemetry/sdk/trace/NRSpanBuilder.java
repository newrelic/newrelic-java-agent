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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class NRSpanBuilder implements SpanBuilder {
    private final Instrumentation instrumentation;
    private final String spanName;
    private final Map<String, Object> attributes = new HashMap<>();
    private final TracerSharedState sharedState;
    private final Consumer<ExitTracerSpan> endHandler;
    private final InstrumentationLibraryInfo instrumentationLibraryInfo;
    private SpanKind spanKind= SpanKind.INTERNAL;
    private SpanContext parentSpanContext;

    public NRSpanBuilder(Instrumentation instrumentation, String instrumentationScopeName, String instrumentationScopeVersion, TracerSharedState sharedState, String spanName) {
        this.instrumentation = instrumentation;
        this.spanName = spanName;
        this.sharedState = sharedState;
        instrumentationLibraryInfo = InstrumentationLibraryInfo.create(instrumentationScopeName, instrumentationScopeVersion);
        attributes.put(ExitTracerSpan.OTEL_LIBRARY_NAME.getKey(), instrumentationScopeName);
        if (instrumentationScopeVersion != null) {
            attributes.put(ExitTracerSpan.OTEL_LIBRARY_VERSION, instrumentationScopeVersion);
        }
        if (sharedState.getActiveSpanProcessor().isEndRequired()) {
            endHandler = sharedState.getActiveSpanProcessor()::onEnd;
        } else {
            endHandler = span -> {};
        }
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
        // REVIEW - we're not picking up the global resources
        return onStart(new ExitTracerSpan(tracer, instrumentationLibraryInfo, spanKind, spanName, parentSpanContext, sharedState.getResource(), attributes, endHandler));
    }

    private Span startServerSpan(SpanContext parentSpanContext) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(true);
        final ExtendedRequest request = new ExtendedRequest() {

            @Override
            public String getRequestURI() {
                Object httpRoute = attributes.get("http.route");
                if (httpRoute != null) {
                    return httpRoute.toString();
                }
                return (String) attributes.get("url.path");
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
                if ("User-Agent".equals(name)) {
                    return (String) attributes.get("user_agent.original");
                }
                return null;
            }

            @Override
            public String getMethod() {
                return (String) attributes.get("http.request.method");
            }
        };
        final ExtendedResponse response = new ExtendedResponse() {

            @Override
            public int getStatus() throws Exception {
                Object statusCode = attributes.get("http.response.status_code");
                return statusCode instanceof Number ? ((Number)statusCode).intValue() : 0;
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
        return onStart(new ExitTracerSpan((ExitTracer) tracedMethod, instrumentationLibraryInfo, spanKind, spanName,
                parentSpanContext, sharedState.getResource(), attributes, endHandler));
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
