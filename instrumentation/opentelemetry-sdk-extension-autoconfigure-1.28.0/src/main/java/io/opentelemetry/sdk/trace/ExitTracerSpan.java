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
import com.newrelic.agent.bridge.opentelemetry.SpanEvent;
import com.newrelic.agent.bridge.opentelemetry.SpanLink;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.nr.agent.instrumentation.utils.AttributesHelper;
import com.nr.agent.instrumentation.utils.span.AttributeMapper;
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
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.internal.AttributeUtil;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
    private final Object lock = new Object();
    // otel.scope.version and otel.scope.name should be reported along with the deprecated versions otel.library.version and otel.library.name
    static final String OTEL_SCOPE_VERSION = "otel.scope.version";
    static final AttributeKey<String> OTEL_SCOPE_NAME = AttributeKey.stringKey("otel.scope.name");
    static final String OTEL_LIBRARY_VERSION = "otel.library.version";
    static final AttributeKey<String> OTEL_LIBRARY_NAME = AttributeKey.stringKey("otel.library.name");

    private static final AttributeKey<String> DB_SYSTEM = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> DB_STATEMENT = AttributeKey.stringKey("db.statement");
    private static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");
    private static final AttributeKey<String> DB_SQL_TABLE = AttributeKey.stringKey("db.sql.table");

    private static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
    private static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");

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
    private final AttributeMapper attributeMapper = AttributeMapper.getInstance();
    private final List<LinkData> links;
    private final int totalNumberOfLinksAdded;
    private List<EventData> events;
    private int totalNumberOfEventsAdded;
    private final AnchoredClock clock;

    private static final int MAX_EVENTS_PER_SPAN = 100;
    private static final int MAX_EVENT_ATTRIBUTES = 64;
    private static final int MAX_EVENT_ATTRIBUTE_LENGTH = 255;

    ExitTracerSpan(ExitTracer tracer, InstrumentationLibraryInfo instrumentationLibraryInfo, SpanKind spanKind, String spanName, SpanContext parentSpanContext,
            Resource resource, Map<String, Object> attributes, Consumer<ExitTracerSpan> onEnd, List<LinkData> links, int totalNumberOfLinksAdded) {
        this.tracer = tracer;
        this.spanKind = spanKind;
        this.spanName = spanName;
        this.parentSpanContext = parentSpanContext;
        this.attributes = attributes;
        this.onEnd = onEnd;
        this.resource = resource;
        this.instrumentationLibraryInfo = instrumentationLibraryInfo;
        this.startEpochNanos = System.nanoTime();
        this.links = links;
        this.spanContext = SpanContext.create(tracer.getTraceId(), tracer.getSpanId(), TraceFlags.getDefault(), TraceState.getDefault());
        this.setAllAttributes(resource.getAttributes());
        this.totalNumberOfLinksAdded = totalNumberOfLinksAdded;
        this.totalNumberOfEventsAdded = 0;
        this.ended = false;
        this.clock = AnchoredClock.create(Clock.getDefault());
    }

    public static ExitTracerSpan wrap(ExitTracer tracer) {
        return new ExitTracerSpan(tracer, InstrumentationLibraryInfo.empty(), SpanKind.INTERNAL, tracer.getMetricName(), SpanContext.getInvalid(),
                Resource.empty(), Collections.emptyMap(), span -> {
        }, Collections.emptyList(), 0);
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
        attributes.put(key.getKey(), value);
        return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
        if (name == null) {
            return this;
        } else {
            if (attributes == null) {
                attributes = Attributes.empty();
            }

            int totalAttributeCount = attributes.size();
            this.addTimedEvent(
                    EventData.create(this.clock.now(), name, AttributeUtil.applyAttributesLimit(attributes, MAX_EVENT_ATTRIBUTES, MAX_EVENT_ATTRIBUTE_LENGTH),
                            totalAttributeCount));
            return this;
        }
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
        if (name != null && unit != null) {
            if (attributes == null) {
                attributes = Attributes.empty();
            }

            int totalAttributeCount = attributes.size();
            this.addTimedEvent(EventData.create(unit.toNanos(timestamp), name,
                    AttributeUtil.applyAttributesLimit(attributes, MAX_EVENT_ATTRIBUTES, MAX_EVENT_ATTRIBUTE_LENGTH), totalAttributeCount));
            return this;
        } else {
            return this;
        }
    }

    private void addTimedEvent(EventData timedEvent) {
        synchronized (this.lock) {
            ++this.totalNumberOfEventsAdded;
            if (!this.ended) {
                if (this.events == null) {
                    this.events = new ArrayList<>(MAX_EVENTS_PER_SPAN);
                }

                if (this.events.size() != MAX_EVENTS_PER_SPAN) {
                    this.events.add(timedEvent);
                } else {
                    AgentBridge.getAgent()
                            .getLogger()
                            .log(Level.FINEST, "Unable to add span event because the limit of " + MAX_EVENTS_PER_SPAN + " events per span has been reached.");
                }
            }
        }
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
        copySpanLinksToTracer(links);
        List<EventData> immutableEvents = this.events == null ? Collections.emptyList() : Collections.unmodifiableList(this.events);
        copySpanEventsToTracer(immutableEvents);
        tracer.finish();
        endEpochNanos = System.nanoTime();
        ended = true;
        onEnd.accept(this);
    }

    // TODO add status.code and status.description

    private void copySpanLinksToTracer(List<LinkData> links) {
        if (links != null && !links.isEmpty()) {
            for (LinkData linkData : links) {
                String id = tracer.getSpanId();
                String traceId = tracer.getTraceId();
                String linkedSpanId = linkData.getSpanContext().getSpanId();
                String linkedTraceId = linkData.getSpanContext().getTraceId();
                Map<String, Object> linkDataAttributes = toMap(linkData.getAttributes());
                tracer.addSpanLink(new SpanLink(this.startEpochNanos, id, traceId, linkedSpanId, linkedTraceId, linkDataAttributes));
            }
        }
    }

    private void copySpanEventsToTracer(List<EventData> events) {
        if (events != null && !events.isEmpty()) {
            for (EventData eventData : events) {
                String spanId = tracer.getSpanId();
                String traceId = tracer.getTraceId();
                String name = eventData.getName();
                Map<String, Object> eventDataAttributes = toMap(eventData.getAttributes());
                tracer.addSpanEvent(new SpanEvent(this.startEpochNanos, name, traceId, spanId, eventDataAttributes));
            }
        }
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
        final String dbSystem = getAttribute(generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.DBSystem));
        if (dbSystem != null) {
            String operation = getAttribute(generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.DBOperation));
            DatastoreParameters.InstanceParameter builder = DatastoreParameters
                    .product(dbSystem)
                    .collection(getAttribute(generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.DBTable)))
                    .operation(operation == null ? "unknown" : operation);
            String serverAddress = getAttribute(generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.Host));
            Long serverPort = getAttribute(generateLongAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.Port));

            DatastoreParameters.DatabaseParameter instance = serverAddress == null ? builder.noInstance() :
                    builder.instance(serverAddress, (serverPort == null ? Long.valueOf(0L) : serverPort).intValue());

            String dbName = getAttribute(generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.DBName));
            DatastoreParameters.SlowQueryParameter slowQueryParameter =
                    dbName == null ? instance.noDatabaseName() : instance.databaseName(dbName);
            final String dbStatement = getAttribute(
                    generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.DBStatement));
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

    private AttributeKey<String> generateStringAttributeKey(SpanKind spanKind, com.nr.agent.instrumentation.utils.span.AttributeType attributeType) {
        return AttributeKey.stringKey(
                attributeMapper.findProperOtelKey(spanKind, attributeType, attributes.keySet()));
    }

    private AttributeKey<Long> generateLongAttributeKey(SpanKind spanKind, com.nr.agent.instrumentation.utils.span.AttributeType attributeType) {
        return AttributeKey.longKey(
                attributeMapper.findProperOtelKey(spanKind, attributeType, attributes.keySet()));
    }

    String getProcedure() {
        AttributeKey<String> key = generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.ExternalProcedure);
        if (key.getKey().isEmpty()) {
            key = generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.Method);
        }
        return key.getKey().isEmpty() ? "unknown" : getAttribute(key);
    }

    URI getUri() throws URISyntaxException {
        final String urlFull = getAttribute(generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.Route));
        if (urlFull != null) {
            return URI.create(urlFull);
        } else {
            final String serverAddress = getAttribute(generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.Host));
            if (serverAddress != null) {
                final String scheme = getAttribute(generateStringAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.Route));
                final Long serverPort = getAttribute(generateLongAttributeKey(SpanKind.CLIENT, com.nr.agent.instrumentation.utils.span.AttributeType.Port));
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
            return events;
        }

        @Override
        public List<LinkData> getLinks() {
            return links;
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
            return totalNumberOfEventsAdded;
        }

        @Override
        public int getTotalRecordedLinks() {
            return totalNumberOfLinksAdded;
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
