/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.datastore.SqlQueryConverter;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.nr.agent.instrumentation.utils.AttributesHelper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Representation of a Span
 */
public class ExitTracerSpan implements ReadWriteSpan {
    static final String OTEL_LIBRARY_VERSION = "otel.library.version";
    static final AttributeKey<String> OTEL_LIBRARY_NAME = AttributeKey.stringKey("otel.library.name");
    private static final AttributeKey<String> DB_SYSTEM = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> DB_STATEMENT = AttributeKey.stringKey("db.statement");
    private static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");
    private static final AttributeKey<String> DB_SQL_TABLE = AttributeKey.stringKey("db.sql.table");
    private static final AttributeKey<String> DB_NAME = AttributeKey.stringKey("db.name");

    private static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
    private static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");
    private static final AttributeKey<String> URL_FULL = AttributeKey.stringKey("url.full");
    private static final AttributeKey<String> URL_SCHEME = AttributeKey.stringKey("url.scheme");
    private static final AttributeKey<String> RPC_METHOD = AttributeKey.stringKey("rpc.method");
    private static final AttributeKey<String> CODE_FUNCTION = AttributeKey.stringKey("code.function");
    private static final AttributeKey<String> HTTP_REQUEST_METHOD = AttributeKey.stringKey("http.request.method");

    private static final List<AttributeKey<String>> PROCEDURE_KEYS =
            Arrays.asList(CODE_FUNCTION, RPC_METHOD, HTTP_REQUEST_METHOD);
    // these attributes are reported as agent attributes, we don't want to duplicate them in user attributes
    private static final Set<String> AGENT_ATTRIBUTE_KEYS =
            Collections.unmodifiableSet(
                    Stream.of(DB_STATEMENT, DB_SQL_TABLE, DB_SYSTEM, DB_OPERATION, SERVER_ADDRESS, SERVER_PORT)
                            .map(AttributeKey::getKey)
                            .collect(Collectors.toSet()));

    final ExitTracer tracer;
    private final SpanKind spanKind;
    private final InstrumentationLibraryInfo instrumentationLibraryInfo;
    private final Map<String, Object> attributes;
    private final SpanContext spanContext;
    private final Consumer<ExitTracerSpan> onEnd;
    private final SpanContext parentSpanContext;
    private final long startEpochNanos;
    private boolean ended;
    private String spanName;
    private long endEpochNanos;
    private final Resource resource;

    ExitTracerSpan(ExitTracer tracer, InstrumentationLibraryInfo instrumentationLibraryInfo, SpanKind spanKind, String spanName, SpanContext parentSpanContext,
            Resource resource, Map<String, Object> attributes, Consumer<ExitTracerSpan> onEnd) {
        this.tracer = tracer;
        this.spanKind = spanKind;
        this.spanName = spanName;
        this.parentSpanContext = parentSpanContext;
        this.attributes = attributes;
        this.onEnd = onEnd;
        this.resource = resource;
        this.instrumentationLibraryInfo = instrumentationLibraryInfo;
        this.startEpochNanos = System.nanoTime();
        this.spanContext = SpanContext.create(tracer.getTraceId(), tracer.getSpanId(), TraceFlags.getDefault(), TraceState.getDefault());
        this.setAllAttributes(resource.getAttributes());
    }

    public static ExitTracerSpan wrap(ExitTracer tracer) {
        return new ExitTracerSpan(tracer, InstrumentationLibraryInfo.empty(), SpanKind.INTERNAL, tracer.getMetricName(), SpanContext.getInvalid(),
                Resource.empty(), Collections.emptyMap(), span -> {
        });
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
        attributes.put(key.getKey(), value);
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
    public Span recordException(Throwable exception) {
        NewRelic.noticeError(exception);
        return this;
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
        NewRelic.noticeError(exception, toMap(additionalAttributes));
        return this;
    }

    static Map<String, Object> toMap(Attributes attributes) {
        final Map<String, Object> map = new HashMap<>(attributes.size());
        attributes.forEach((key, value) -> {
            switch (key.getType()) {
                case STRING:
                case LONG:
                case DOUBLE:
                case BOOLEAN:
                    map.put(key.getKey(), value);
                    break;
            }
        });
        return map;
    }

    @Override
    public Span updateName(String name) {
        this.spanName = name;
        return this;
    }

    @Override
    public void end() {
        if (SpanKind.CLIENT == spanKind) {
            reportClientSpan();
        }
        tracer.setMetricName("Span", spanName);
        // db.statement is reported through DatastoreParameters.SlowQueryParameter.  That code path
        // will correctly obfuscate the sql based on agent settings.
        Map<String, Object> filteredAttributes = attributes.entrySet().stream()
                .filter(entry -> !AGENT_ATTRIBUTE_KEYS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        tracer.addCustomAttributes(filteredAttributes);
        tracer.finish();
        endEpochNanos = System.nanoTime();
        ended = true;
        onEnd.accept(this);
    }

    @Override
    public void end(long timestamp, TimeUnit unit) {
        this.end();
    }

    @Override
    public SpanContext getSpanContext() {
        return spanContext;
    }

    @Override
    public boolean isRecording() {
        return true;
    }

    @Override
    public SpanContext getParentSpanContext() {
        return parentSpanContext;
    }

    @Override
    public String getName() {
        return spanName;
    }

    @Override
    public SpanData toSpanData() {
        return new BasicSpanData(spanName, endEpochNanos, AttributesHelper.toAttributes(attributes), ended);
    }

    @Override
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
        return InstrumentationLibraryInfo.empty();
    }

    @Override
    public boolean hasEnded() {
        return ended;
    }

    @Override
    public long getLatencyNanos() {
        long endEpochNanos = ended ? this.endEpochNanos : System.nanoTime();
        return endEpochNanos - startEpochNanos;
    }

    @Override
    public SpanKind getKind() {
        return spanKind;
    }

    public <T> T getAttribute(AttributeKey<T> key) {
        Object value = attributes.get(key.getKey());
        if (key.getType() == AttributeType.LONG && value instanceof Number) {
            value = ((Number) value).longValue();
        } else if (key.getType() == AttributeType.DOUBLE && value instanceof Number) {
            value = ((Number) value).doubleValue();
        }
        return (T) value;
    }

    private void reportClientSpan() {
        final String dbSystem = getAttribute(DB_SYSTEM);
        if (dbSystem != null) {
            String operation = getAttribute(DB_OPERATION);
            DatastoreParameters.InstanceParameter builder = DatastoreParameters
                    .product(dbSystem)
                    .collection(getAttribute(DB_SQL_TABLE))
                    .operation(operation == null ? "unknown" : operation);
            String serverAddress = getAttribute(SERVER_ADDRESS);
            Long serverPort = getAttribute(SERVER_PORT);

            DatastoreParameters.DatabaseParameter instance = serverAddress == null ? builder.noInstance() :
                    builder.instance(serverAddress, (serverPort == null ? Long.valueOf(0L) : serverPort).intValue());

            String dbName = getAttribute(DB_NAME);
            DatastoreParameters.SlowQueryParameter slowQueryParameter =
                    dbName == null ? instance.noDatabaseName() : instance.databaseName(dbName);
            final String dbStatement = getAttribute(DB_STATEMENT);
            final DatastoreParameters datastoreParameters;
            if (dbStatement == null) {
                datastoreParameters = slowQueryParameter.build();
            } else {
                datastoreParameters = slowQueryParameter.slowQuery(dbStatement, SqlQueryConverter.INSTANCE).build();
            }

            tracer.reportAsExternal(datastoreParameters);
        }
        // Only support the current otel spec.  Ignore client spans with old attribute names
        else {
            try {
                final URI uri = getUri();
                if (uri != null) {
                    final String libraryName = getAttribute(OTEL_LIBRARY_NAME);
                    HttpParameters genericParameters = HttpParameters.library(libraryName).uri(uri)
                            .procedure(getProcedure()).noInboundHeaders().build();
                    tracer.reportAsExternal(genericParameters);
                }
            } catch (URISyntaxException e) {
                NewRelic.getAgent().getLogger().log(Level.FINER, "Error parsing client span uri", e);
            }
        }
    }

    String getProcedure() {
        for (AttributeKey<String> key : PROCEDURE_KEYS) {
            String value = getAttribute(key);
            if (value != null) {
                return value;
            }
        }
        return "unknown";
    }

    URI getUri() throws URISyntaxException {
        final String urlFull = getAttribute(URL_FULL);
        if (urlFull != null) {
            return URI.create(urlFull);
        } else {
            final String serverAddress = getAttribute(SERVER_ADDRESS);
            if (serverAddress != null) {
                final String scheme = getAttribute(URL_SCHEME);
                final Long serverPort = getAttribute(SERVER_PORT);
                return new URI(scheme == null ? "http" : scheme, null, serverAddress,
                        serverPort == null ? 0 : serverPort.intValue(), null, null, null);
            }
        }
        return null;
    }

    public Scope createScope(Scope scope) {
        final Token token = tracer.getToken();
        // we can't link a known transaction from one thread to another unless there is
        // a transaction with at least one tracer on the new thread.
        AgentBridge.getAgent().getTransaction(true);
        final ExitTracer tracer = AgentBridge.instrumentation.createTracer(null,
                TracerFlags.CUSTOM | TracerFlags.ASYNC);
        tracer.setMetricName("Java", "OpenTelemetry", "AsyncScope");
        token.link();
        return () -> {
            token.expire();
            tracer.finish();
            scope.close();
        };
    }

    public class BasicSpanData implements SpanData {
        private final String spanName;
        private final long endEpochNanos;
        private final Attributes attributes;
        private final boolean ended;

        public BasicSpanData(String spanName, long endEpochNanos, Attributes attributes, boolean ended) {
            this.spanName = spanName;
            this.endEpochNanos = endEpochNanos;
            this.attributes = attributes;
            this.ended = ended;
        }

        @Override
        public String getName() {
            return spanName;
        }

        @Override
        public SpanKind getKind() {
            return spanKind;
        }

        @Override
        public SpanContext getSpanContext() {
            return spanContext;
        }

        @Override
        public SpanContext getParentSpanContext() {
            return parentSpanContext;
        }

        @Override
        public StatusData getStatus() {
            return StatusData.ok();
        }

        @Override
        public long getStartEpochNanos() {
            return startEpochNanos;
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public List<EventData> getEvents() {
            return Collections.emptyList();
        }

        @Override
        public List<LinkData> getLinks() {
            return Collections.emptyList();
        }

        @Override
        public long getEndEpochNanos() {
            return endEpochNanos;
        }

        @Override
        public boolean hasEnded() {
            return ended;
        }

        @Override
        public int getTotalRecordedEvents() {
            return 0;
        }

        @Override
        public int getTotalRecordedLinks() {
            return 0;
        }

        @Override
        public int getTotalAttributeCount() {
            return 0;
        }

        @Override
        public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
            return instrumentationLibraryInfo;
        }

        @Override
        public Resource getResource() {
            return resource;
        }
    }
}
