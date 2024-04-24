package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class SpanToTracerProcessor implements SpanProcessor {
    private final Map<String, ExitTracer> tracersBySpanId = new ConcurrentHashMap<>();
    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        final ExitTracer tracer = AgentBridge.instrumentation.createTracer("Span/" + span.getName(),
                TracerFlags.GENERATE_SCOPED_METRIC
                        | TracerFlags.TRANSACTION_TRACER_SEGMENT
                        | TracerFlags.CUSTOM);
        if (tracer != null) {
            tracer.addCustomAttribute("span.kind", span.getKind().name());
            tracersBySpanId.put(span.getSpanContext().getSpanId(), tracer);

            NewRelic.getAgent().getMetricAggregator().recordMetric("Supportability/SpanToTracerProcessor/tracersBySpanId/size", tracersBySpanId.size());
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "No tracer, skipping span {0}", span.getName());
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        final ExitTracer tracer = tracersBySpanId.remove(span.getSpanContext().getSpanId());
        if (tracer != null) {
            tracer.addCustomAttribute("span.name", span.getName());
            tracer.finish();
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
