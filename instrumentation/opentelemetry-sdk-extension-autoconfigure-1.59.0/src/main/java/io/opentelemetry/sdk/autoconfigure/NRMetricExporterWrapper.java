/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.nr.agent.instrumentation.utils.audit.OtlpAuditLogger;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

import static com.newrelic.agent.util.LicenseKeyUtil.obfuscateLicenseKey;

/**
 * A delegating MetricExporter that intercepts export() calls to rewrite MetricData
 * with updated Resource attributes from the agent's service metadata.
 *
 * <p>Service metadata is populated from the connect response and
 * may change on agent reconnect. This wrapper ensures exported metrics always carry
 * the latest metadata as Resource attributes.
 *
 * <p>When the agent's audit_mode is enabled, each export call produces one audit log
 * entry for the outbound request. Additionally, data usage metrics are always recorded
 * when the export completes.
 */
final class NRMetricExporterWrapper implements MetricExporter {

    private static final String EXPORT_SUCCESS_METRIC = "Supportability/Metrics/Java/OpenTelemetryBridge/export/success";
    private static final String EXPORT_FAILURE_METRIC = "Supportability/Metrics/Java/OpenTelemetryBridge/export/failure";

    private final MetricExporter delegate;
    private final String endpoint;
    private volatile Map<String, String> lastMetadata;
    private volatile Resource cachedOverlayResource;

    NRMetricExporterWrapper(MetricExporter delegate, String endpoint) {
        this.delegate = delegate;
        this.endpoint = endpoint;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        boolean auditMode = OtlpAuditLogger.isAuditModeEnabled();
        Collection<MetricData> toExport = prepareMetrics(metrics);
        final int bytesSent = logAuditRequest(toExport, auditMode);
        final CompletableResultCode result = delegate.export(toExport);
        result.whenComplete(new Runnable() {
            @Override
            public void run() {
                /*
                 * Unfortunately, response bytes received are not accessible in this
                 * instrumentation module, so we always set them to zero. This should
                 * largely be fine though, as bytes received is typically zero and
                 * the actual value is logged via the exporter transport layer
                 * instrumentation when audit_mode is enabled. Bytes sent is the
                 * important value to capture.
                 */
                OtlpAuditLogger.recordDataUsageMetrics(bytesSent, 0);
                // The OTel SDK exhausts its retry budget inside delegate.export(), so a failure
                // here means the batch was dropped after retries (or was non-retryable), not that
                // a retry is still pending.
                NewRelic.incrementCounter(result.isSuccess() ? EXPORT_SUCCESS_METRIC : EXPORT_FAILURE_METRIC);
            }
        });
        return result;
    }

    private Collection<MetricData> prepareMetrics(Collection<MetricData> metrics) {
        Map<String, String> currentMetadata = AgentBridge.getAgent().getServiceMetadata();
        if (currentMetadata == null || currentMetadata.isEmpty()) {
            return metrics;
        }
        // Rebuild overlay resource only when metadata reference changes
        if (currentMetadata != lastMetadata) {
            lastMetadata = currentMetadata;
            ResourceBuilder builder = Resource.builder();
            for (Map.Entry<String, String> entry : currentMetadata.entrySet()) {
                builder.put(entry.getKey(), entry.getValue());
            }
            cachedOverlayResource = builder.build();
        }
        Resource overlay = cachedOverlayResource;
        if (overlay == null) {
            return metrics;
        }
        Collection<MetricData> rewritten = new ArrayList<MetricData>(metrics.size());
        for (MetricData metric : metrics) {
            Resource merged = metric.getResource().merge(overlay);
            rewritten.add(new ResourceOverlayMetricData(metric, merged));
        }
        return rewritten;
    }

    private int logAuditRequest(Collection<MetricData> metrics, boolean auditMode) {
        try {
            MetricsRequestMarshaler marshaler = MetricsRequestMarshaler.create(metrics);
            int bytes = marshaler.getBinarySerializedSize();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytes);
            marshaler.writeBinaryTo(outputStream);
            String payload = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            if (auditMode) {
                NewRelic.getAgent().getLogger().log(Level.INFO,
                        "Sent OTLP/Metrics to: {0}, bytes: {1}, payload: {2}",
                        obfuscateLicenseKey(endpoint == null ? "unknown" : endpoint),
                        bytes,
                        payload);
            }
            return bytes;
        } catch (Exception e) {
            if (auditMode) {
                NewRelic.getAgent().getLogger().log(Level.FINE,
                        "Audit: failed to serialize OTLP/Metrics request for logging: {0}", e.getMessage());
            }
            return 0;
        }
    }

    @Override
    public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
        return delegate.getDefaultAggregation(instrumentType);
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return delegate.getAggregationTemporality(instrumentType);
    }

    @Override
    public MemoryMode getMemoryMode() {
        return delegate.getMemoryMode();
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
