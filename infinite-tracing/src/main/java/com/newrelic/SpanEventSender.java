package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

class SpanEventSender implements Runnable {

    private final Logger logger;
    private final BlockingQueue<SpanEvent> queue;
    private final MetricAggregator aggregator;
    private final ChannelManager channelManager;

    SpanEventSender(InfiniteTracingConfig config, BlockingQueue<SpanEvent> queue, MetricAggregator aggregator, ChannelManager channelManager) {
        this.logger = config.getLogger();
        this.queue = queue;
        this.aggregator = aggregator;
        this.channelManager = channelManager;
    }

    @Override
    public void run() {
        logger.log(Level.FINE, "Initializing {0}", this.getClass().getSimpleName());
        while (true) {
            try {
                pollAndWrite();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, t, "A problem occurred and no further spans will be sent.");
                return;
            }
        }
    }

    @VisibleForTesting
    void pollAndWrite() {
        // Get stream observer
        ClientCallStreamObserver<V1.Span> observer = channelManager.getSpanObserver();

        // Confirm the observer is ready
        if (!awaitReadyObserver(observer)) {
            return;
        }

        // Poll queue for span
        SpanEvent span = pollSafely();
        if (span == null) {
            return;
        }

        // Convert span and write to observer
        V1.Span convertedSpan = convert(span);
        writeToObserver(observer, convertedSpan);
    }

    @VisibleForTesting
    boolean awaitReadyObserver(ClientCallStreamObserver<V1.Span> observer) {
        if (observer.isReady()) {
            return true;
        }
        try {
            logger.log(Level.FINE, "Waiting for gRPC span observer to be ready.");
            aggregator.incrementCounter("Supportability/InfiniteTracing/NotReady");
            Thread.sleep(250);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while awaiting ready gRPC span observer.");
        }
        return false;
    }

    @VisibleForTesting
    SpanEvent pollSafely() {
        try {
            return queue.poll(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while polling for spans.");
        }
    }

    @VisibleForTesting
    static V1.Span convert(SpanEvent spanEvent) {
        Map<String, V1.AttributeValue> intrinsicAttributes = copyAttributes(spanEvent.getIntrinsics());
        Map<String, V1.AttributeValue> userAttributes = copyAttributes(spanEvent.getUserAttributesCopy());
        Map<String, V1.AttributeValue> agentAttributes = copyAttributes(spanEvent.getAgentAttributes());

        intrinsicAttributes.put("appName", V1.AttributeValue.newBuilder().setStringValue(spanEvent.getAppName()).build());

        return V1.Span.newBuilder()
                .setTraceId(spanEvent.getTraceId())
                .putAllIntrinsics(intrinsicAttributes)
                .putAllAgentAttributes(agentAttributes)
                .putAllUserAttributes(userAttributes)
                .build();
    }

    private static Map<String, V1.AttributeValue> copyAttributes(Map<String, Object> original) {
        Map<String, V1.AttributeValue> copy = new HashMap<>();
        if (original == null) {
            return copy;
        }

        for (Map.Entry<String, Object> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                copy.put(entry.getKey(), V1.AttributeValue.newBuilder().setStringValue((String) value).build());
            } else if (value instanceof Long || value instanceof Integer) {
                copy.put(entry.getKey(), V1.AttributeValue.newBuilder().setIntValue(((Number) value).longValue()).build());
            } else if (value instanceof Float || value instanceof Double) {
                copy.put(entry.getKey(), V1.AttributeValue.newBuilder().setDoubleValue(((Number) value).doubleValue()).build());
            } else if (value instanceof Boolean) {
                copy.put(entry.getKey(), V1.AttributeValue.newBuilder().setBoolValue((Boolean) value).build());
            }
        }
        return copy;
    }

    @VisibleForTesting
    void writeToObserver(ClientCallStreamObserver<V1.Span> observer, V1.Span span) {
        try {
            observer.onNext(span);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, t, "Unable to send span.");
            throw t;
        }
        aggregator.incrementCounter("Supportability/InfiniteTracing/Span/Sent");
    }

}