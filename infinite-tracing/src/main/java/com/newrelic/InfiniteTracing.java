package com.newrelic;

import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.MetricAggregator;

import java.util.Collections;
import java.util.Map;

public class InfiniteTracing implements Consumer<SpanEvent> {
    private final SpanEventConsumer spanEventConsumer;

    /**
     * Set up the Infinite Tracing library.
     * @param config Required data to start a connection to Infinite Tracing.
     * @param metricAggregator Aggregator to which information about the library will be recorded.
     * @return An object that requires two final pieces: {@link #setConnectionMetadata} and {@link #start}
     */
    @SuppressWarnings("unused") // this is the public API of the class
    public static InfiniteTracing initialize(InfiniteTracingConfig config, MetricAggregator metricAggregator) {
        SpanEventConsumer spanEventConsumer = SpanEventConsumer.builder(config, metricAggregator).build();
        return new InfiniteTracing(spanEventConsumer);
    }

    private InfiniteTracing(SpanEventConsumer spanEventConsumer) {
        this.spanEventConsumer = spanEventConsumer;
    }

    /**
     * Call this method when the connection metadata changes, which is driven by the collector.
     *
     * @param newRunToken The new agent run token that should be supplied to the Trace Observer.
     * @param headers The metadata that should be supplied to the Trace Observer as headers.
     */
    public void setConnectionMetadata(String newRunToken, Map<String, String> headers) {
        spanEventConsumer.setConnectionMetadata(newRunToken, headers);
    }

    /**
     * Initiates the connection and acceptance of {@link SpanEvent} instances.
     */
    @SuppressWarnings("unused") // this is the public API of the class
    public void start() {
        spanEventConsumer.start();
    }

    /**
     * Call this method whenever the run token changes.
     * @deprecated use {@link #setConnectionMetadata} instead
     */
    @Deprecated
    public void setRunToken(String newRunToken) {
        setConnectionMetadata(newRunToken, Collections.<String, String>emptyMap());
    }

    @Override
    public void accept(SpanEvent spanEvent) {
        spanEventConsumer.accept(spanEvent);
    }
}
