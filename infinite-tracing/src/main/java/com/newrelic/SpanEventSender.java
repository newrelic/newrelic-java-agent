package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;

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

    /**
     * Run a continuous loop polling the {@link #queue} for the next span event and writing it
     * to the Infinite Trace Observer via gRPC.
     */
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
        V1.Span convertedSpan = SpanConverter.convert(span);
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