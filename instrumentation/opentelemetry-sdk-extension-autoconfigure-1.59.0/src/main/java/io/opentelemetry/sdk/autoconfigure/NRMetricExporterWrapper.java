/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * A delegating MetricExporter that intercepts export() calls to rewrite MetricData
 * with updated Resource attributes from the agent's service metadata.
 *
 * <p>Service metadata is populated from the connect response and
 * may change on agent reconnect. This wrapper ensures exported metrics always carry
 * the latest metadata as Resource attributes.
 */
final class NRMetricExporterWrapper implements MetricExporter {

    private final MetricExporter delegate;
    private volatile Map<String, String> lastMetadata;
    private volatile Resource cachedOverlayResource;

    NRMetricExporterWrapper(MetricExporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        Map<String, String> currentMetadata = AgentBridge.getAgent().getServiceMetadata();
        if (currentMetadata == null || currentMetadata.isEmpty()) {
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
            return delegate.export(metrics);
        }

        Collection<MetricData> rewritten = new ArrayList<MetricData>(metrics.size());
        for (MetricData metric : metrics) {
            Resource merged = metric.getResource().merge(overlay);
            rewritten.add(new ResourceOverlayMetricData(metric, merged));
        }
        return delegate.export(rewritten);
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
