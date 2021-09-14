package com.newrelic;

import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static java.util.stream.Collectors.toList;

class OtlpExporter implements Exporter {

    private final Logger logger;
    private final OtlpGrpcSpanExporter otlpSpanExporter;
    private final long timeoutSeconds = 10;
    private final InstrumentationLibraryInfo instrumentationLibraryInfo;
    private final Object lock = new Object();
    @GuardedBy("lock") private Resource resource;

    OtlpExporter(InfiniteTracingConfig config, String agentRunToken, Map<String, String> requestMetadata) {
        logger = config.getLogger();
        otlpSpanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(config.getOtlpEndpoint())
                .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
        instrumentationLibraryInfo = InstrumentationLibraryInfo.create(
                OtlpExporter.class.getPackage().getImplementationTitle(),
                OtlpExporter.class.getPackage().getImplementationVersion());
        setResource(agentRunToken, requestMetadata);
    }

    @Override
    public void updateMetadata(String agentRunToken, Map<String, String> requestMetadata) {
        setResource(agentRunToken, requestMetadata);
    }

    private void setResource(String agentRunToken, Map<String, String> requestMetadata) {
        synchronized (lock) {
            ResourceBuilder builder = Resource.builder().put("agent_run_token", agentRunToken);
            builder.put("service.name", (String) NewRelic.getAgent().getConfig().getValue("app_name"));
            requestMetadata.forEach(builder::put);
            resource = builder.build();
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public int maxExportSize() {
        return 100;
    }

    @Override
    public void export(Collection<SpanEvent> spanEvents) {
        logger.log(Level.INFO, "Exporting " + spanEvents.size() + " spans via OTLP.");
        Resource resource;
        synchronized (lock) {
            resource = this.resource;
        }
        List<SpanData> spanData = spanEvents.stream()
                .map(spanEvent -> new SpanEventDelegate(spanEvent, resource, instrumentationLibraryInfo))
                .collect(toList());
        CompletableResultCode result = otlpSpanExporter.export(spanData).join(timeoutSeconds, TimeUnit.SECONDS);
        if (!result.isSuccess()) {
            logger.log(Level.WARNING, "Failed to export spans via OTLP.");
        }
    }

    @Override
    public void shutdown() {
        otlpSpanExporter.shutdown();
    }

    private static class SpanEventDelegate implements SpanData {

        private final SpanEvent spanEvent;
        private final Resource resource;
        private final InstrumentationLibraryInfo instrumentationLibraryInfo;
        private final Map<String, Object> userAttributes;
        private final Map<String, Object> intrinsicAttributes;
        private final Map<String, Object> agentAttributes;
        private final Attributes attributes;

        private SpanEventDelegate(SpanEvent spanEvent, Resource resource, InstrumentationLibraryInfo instrumentationLibraryInfo) {
            this.spanEvent = spanEvent;
            this.resource = resource;
            this.instrumentationLibraryInfo = instrumentationLibraryInfo;
            userAttributes = spanEvent.getUserAttributesCopy();
            intrinsicAttributes = spanEvent.getIntrinsics();
            agentAttributes = spanEvent.getAgentAttributes();
            AttributesBuilder builder = Attributes.builder();
            appendAttributes(builder, userAttributes);
            appendAttributes(builder, intrinsicAttributes);
            appendAttributes(builder, agentAttributes);
            attributes = builder.build();
        }

        @Override
        public String getName() {
            return spanEvent.getName();
        }

        @Override
        public SpanKind getKind() {
            SpanCategory category = spanEvent.getCategory();
            SpanKind spanKind = SpanKind.INTERNAL;
            if (SpanCategory.generic.equals(category) && intrinsicAttributes.containsKey("nr.entryPoint")) {
                spanKind = SpanKind.SERVER;
            } else if (SpanCategory.http.equals(category)) {
                spanKind = SpanKind.CLIENT;
            }
            return spanKind;
        }

        @Override
        public SpanContext getSpanContext() {
            return SpanContext.create(spanEvent.getTraceId(), spanEvent.getGuid(), TraceFlags.getDefault(), TraceState.getDefault());
        }

        @Override
        public SpanContext getParentSpanContext() {
            return SpanContext.create(spanEvent.getTraceId(), spanEvent.getParentId(), TraceFlags.getDefault(), TraceState.getDefault());
        }

        @Override
        public StatusData getStatus() {
            boolean containsError = agentAttributes.keySet().stream().anyMatch(s -> s.startsWith("error."));
            return containsError ? StatusData.error() : StatusData.unset();
        }

        @Override
        public long getStartEpochNanos() {
            return TimeUnit.MILLISECONDS.toNanos(spanEvent.getTimestamp());
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
            return TimeUnit.MILLISECONDS.toNanos(spanEvent.getTimestamp()) + TimeUnit.MICROSECONDS.toNanos((long) spanEvent.getDuration());
        }

        @Override
        public boolean hasEnded() {
            return true;
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
            return attributes.size();
        }

        @Override
        public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
            return instrumentationLibraryInfo;
        }

        @Override
        public Resource getResource() {
            return resource;
        }

        private static void appendAttributes(AttributesBuilder builder, Map<String, Object> attributes) {
            if (attributes == null) {
                return;
            }

            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof String) {
                    builder.put(key, (String) value);
                } else if (value instanceof Long) {
                    builder.put(key, (long) value);
                } else if (value instanceof Integer) {
                    builder.put(key, (int) value);
                } else if (value instanceof Double) {
                    builder.put(key, (double) value);
                } else if (value instanceof Float) {
                    builder.put(key, (float) value);
                } else if (value instanceof Boolean) {
                    builder.put(key, (boolean) value);
                }
            }
        }
    }

}