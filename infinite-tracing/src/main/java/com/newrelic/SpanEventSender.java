package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

class SpanEventSender implements Runnable {

    private final Logger logger;
    private final InfiniteTracingConfig config;
    private final BlockingQueue<SpanEvent> queue;
    private final MetricAggregator aggregator;
    private final ChannelManager channelManager;
    // Destination for agent data
    private static final String INFINITE_TRACING = "InfiniteTracing";
    // Wait for up to 5 seconds for data when batching
    private static final long LINGER_MS = 5000;
    // Allow a maximum batch size of up to 100 items
    private static final int MAX_BATCH_SIZE = 100;

    SpanEventSender(InfiniteTracingConfig config, BlockingQueue<SpanEvent> queue, MetricAggregator aggregator, ChannelManager channelManager) {
        this.logger = config.getLogger();
        this.config = config;
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
        Observer observer = channelManager.getObserver();

        // Confirm the observer is ready
        if (!awaitReadyObserver(observer)) {
            return;
        }

        if (config.getUseBatching()) {
            drainAndSendBatchWhenReady(observer);
        } else {
            pollAndSendSpan(observer);
        }
    }

    @VisibleForTesting
    boolean awaitReadyObserver(Observer observer) {
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
    void drainAndSendBatchWhenReady(Observer observer) {
        // If our queue is larger than our max batch size we will send the batch right away,
        // otherwise we will pause for the linger time to wait for the batch to fill first.
        if (queue.size() < MAX_BATCH_SIZE) {
            try {
                if (queue.isEmpty()) {
                    // Prevent a busy-wait loop when we have no data flowing through
                    Thread.sleep(250);
                } else {
                    Thread.sleep(LINGER_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for span batch to fill.");
            }
        }

        Collection<SpanEvent> spanEvents = drainSpanBatch();
        if (spanEvents.isEmpty()) {
            return;
        }

        // Convert and send the batch to the trace observer
        V1.SpanBatch convertedSpanBatch = SpanConverter.convert(spanEvents);
        writeToObserver(observer, convertedSpanBatch);
    }

    @VisibleForTesting
    Collection<SpanEvent> drainSpanBatch() {
        // Drain up to the max batch size
        Collection<SpanEvent> spanEvents = new LinkedList<>();
        queue.drainTo(spanEvents, MAX_BATCH_SIZE);
        return spanEvents;
    }

    @VisibleForTesting
    void pollAndSendSpan(Observer observer) {
        // Poll queue for span
        SpanEvent span = pollSafely();
        if (span == null) {
            return;
        }

        // Convert single span and write to observer
        V1.Span convertedSpan = SpanConverter.convert(span);
        writeToObserver(observer, convertedSpan);
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
    void writeToObserver(Observer observer, V1.Span span) {
        try {
            observer.onNext(span);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, t, "Unable to send span.");
            throw t;
        }
        aggregator.incrementCounter("Supportability/InfiniteTracing/Span/Sent");
    }

    @VisibleForTesting
    void writeToObserver(Observer observer, V1.SpanBatch spanBatch) {
        try {
            observer.onNext(spanBatch);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, t, "Unable to send span batch.");
            throw t;
        }
        aggregator.incrementCounter("Supportability/InfiniteTracing/Span/Sent", spanBatch.getSpansCount());
    }
}
