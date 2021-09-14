package com.newrelic;

import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.Map;

class NewRelicExporter implements Exporter {

    private final Object lock = new Object();
    private final ChannelManager channelManager;

    @GuardedBy("lock") private ClientCallStreamObserver<V1.Span> observer;

    NewRelicExporter(InfiniteTracingConfig config, MetricAggregator metricAggregator, String agentRunToken, Map<String, String> requestMetadata) {
        this.channelManager = new ChannelManager(config, metricAggregator, agentRunToken, requestMetadata);
    }

    @Override
    public void updateMetadata(String agentRunToken, Map<String, String> requestMetadata) {
        channelManager.updateMetadata(agentRunToken, requestMetadata);
        channelManager.shutdownChannelAndBackoff(0);
    }

    @Override
    public boolean isReady() {
        synchronized (lock) {
            observer = channelManager.getSpanObserver();
            return observer.isReady();
        }
    }

    @Override
    public int maxExportSize() {
        return 1;
    }

    @Override
    public void export(Collection<SpanEvent> spanEvents) {
        synchronized (lock) {
            if (observer == null) {
                throw new IllegalStateException("Cannot export before isReady() is called.");
            }
            // Should only ever be called with a single item
            for (SpanEvent spanEvent : spanEvents) {
                V1.Span convertedSpan = SpanConverter.convert(spanEvent);
                observer.onNext(convertedSpan);
            }
        }
    }

    @Override
    public void shutdown() {
        this.channelManager.shutdownChannelForever();
    }
}
