package com.nr.agent.instrumentation.utils.metrics;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class ServerlessMetricExporter implements MetricExporter {

    private static final CopyOnWriteArrayList<MetricData> totalMetricData = new CopyOnWriteArrayList<>();

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        totalMetricData.addAll(metrics);
        boolean harvested = AgentBridge.serverlessApi.otelHarvest(() -> marshallMetrics(totalMetricData));
        if (harvested) {
            totalMetricData.clear();
        }

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.DELTA;
    }

    private String marshallMetrics(Collection<MetricData> metrics) {
        try {
            MetricsRequestMarshaler marshaler = MetricsRequestMarshaler.create(metrics);
            int bytes = marshaler.getBinarySerializedSize();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytes);
            marshaler.writeBinaryTo(outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Failed to serialize open telemetry metrics", e);
            return null;
        }

    }
}
