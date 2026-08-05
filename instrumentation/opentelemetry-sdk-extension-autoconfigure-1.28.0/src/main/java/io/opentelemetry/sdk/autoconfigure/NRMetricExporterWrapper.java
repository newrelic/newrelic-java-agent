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
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

import static com.newrelic.agent.util.LicenseKeyUtil.obfuscateLicenseKey;
import static com.nr.agent.instrumentation.utils.audit.OtlpAuditLogger.setBytesSent;

/**
 * A delegating MetricExporter that intercepts export() calls to rewrite MetricData
 * with updated Resource attributes from the agent's service metadata.
 *
 * <p>Service metadata is populated from the connect response and
 * may change on agent reconnect. This wrapper ensures exported metrics always carry
 * the latest metadata as Resource attributes.
 *
 * <p>When the agent's audit_mode is enabled, each export call produces one audit log
 * entry for the outbound request.
 */
final class NRMetricExporterWrapper implements MetricExporter {

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
        Map<String, String> currentMetadata = AgentBridge.getAgent().getServiceMetadata();
        if (currentMetadata == null || currentMetadata.isEmpty()) {
            logAuditRequest(metrics);
            return delegate.export(metrics);
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
            logAuditRequest(metrics);
            return delegate.export(metrics);
        }

        Collection<MetricData> rewritten = new ArrayList<MetricData>(metrics.size());
        for (MetricData metric : metrics) {
            Resource merged = metric.getResource().merge(overlay);
            rewritten.add(new ResourceOverlayMetricData(metric, merged));
        }
        logAuditRequest(rewritten);
        return delegate.export(rewritten);
    }

    private void logAuditRequest(Collection<MetricData> metrics) {
        if (!OtlpAuditLogger.isAuditModeEnabled()) {
            return;
        }
        try {
            MetricsRequestMarshaler marshaler = MetricsRequestMarshaler.create(metrics);
            int bytes = marshaler.getBinarySerializedSize();
            setBytesSent(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes);
            marshaler.writeBinaryTo(baos);
            String payload = Base64.getEncoder().encodeToString(baos.toByteArray());
            NewRelic.getAgent().getLogger().log(Level.INFO,
                    "Sent OTLP/Metrics to: {0}, bytes: {1}, payload: {2}",
                    obfuscateLicenseKey(endpoint == null ? "unknown" : endpoint),
                    bytes,
                    payload);
        } catch (IOException e) {
            NewRelic.getAgent().getLogger().log(Level.FINE,
                    "Audit: failed to serialize OTLP/Metrics request for logging: {0}", e.getMessage());
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
