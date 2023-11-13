package com.newrelic.opentelemetry;

import com.newrelic.api.agent.Insights;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.SemanticAttributes;

import java.time.Instant;
import java.util.Map;

final class OpenTelemetryInsights implements Insights {

    private final io.opentelemetry.api.logs.Logger logger;

    private OpenTelemetryInsights(OpenTelemetry openTelemetry) {
        this.logger = openTelemetry.getLogsBridge().get(OpenTelemetryNewRelic.SCOPE_NAME);
    }

    static OpenTelemetryInsights create(OpenTelemetry openTelemetry) {
        return new OpenTelemetryInsights(openTelemetry);
    }

    @Override
    public void recordCustomEvent(String eventType, Map<String, ?> attributesMap) {
        Attributes attributes = OpenTelemetryNewRelic.toAttributes(attributesMap)
                // TODO: is this the right domain?
                .put(SemanticAttributes.EVENT_DOMAIN, "newrelic.api")
                .put(SemanticAttributes.EVENT_NAME, eventType)
                .build();
        // TODO: use event API when stable. For now, its not possible to without taking
        // a dependency on the SDK or accepting EventEmitterProvider as argument during construction.
        logger.logRecordBuilder()
                .setTimestamp(Instant.now())
                .setAllAttributes(attributes)
                .emit();
    }
}
