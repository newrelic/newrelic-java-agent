package com.nr.agent.instrumentation.utils.metrics;

import com.newrelic.agent.bridge.AgentBridge;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;

public class MetricReaderWrapper implements MetricReader {

    public final MetricReader delegate;

    public MetricReaderWrapper(MetricReader delegate) {
        this.delegate = delegate;
    }
    @Override
    public void register(CollectionRegistration collectionRegistration) {
        delegate.register(collectionRegistration);
    }

    @Override
    public CompletableResultCode forceFlush() {
        return delegate.forceFlush();
    }

    @Override
    public CompletableResultCode shutdown() {
        AgentBridge.serverlessApi.removeMetricCollector(this);
        return delegate.shutdown();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return delegate.getAggregationTemporality(instrumentType);
    }
}
