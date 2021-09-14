package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

class SpanEventSender implements Runnable {

    private final Logger logger;
    private final BlockingQueue<SpanEvent> queue;
    private final MetricAggregator aggregator;
    private final Exporter exporter;

    SpanEventSender(Logger logger, BlockingQueue<SpanEvent> queue, MetricAggregator aggregator, Exporter exporter) {
        this.logger = logger;
        this.queue = queue;
        this.aggregator = aggregator;
        this.exporter = exporter;
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
        if (!awaitReadyExporter()) {
            return;
        }

        Collection<SpanEvent> spanEvents = pollSafely(exporter.maxExportSize());
        if (spanEvents.isEmpty()) {
            return;
        }

        try {
            exporter.export(spanEvents);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, t, "Unable to send spans.");
            throw t;
        }
        aggregator.incrementCounter("Supportability/InfiniteTracing/Span/Sent", spanEvents.size());
    }

    @VisibleForTesting
    boolean awaitReadyExporter() {
        if (exporter.isReady()) {
            return true;
        }
        try {
            logger.log(Level.FINE, "Waiting for exporter to be ready.");
            aggregator.incrementCounter("Supportability/InfiniteTracing/NotReady");
            Thread.sleep(250);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while awaiting ready exporter.");
        }
        return false;
    }

    @VisibleForTesting
    Collection<SpanEvent> pollSafely(int maxElements) {
        long end = System.currentTimeMillis() + 250;
        List<SpanEvent> spanEvents = new ArrayList<>();
        int drainedElements = queue.drainTo(spanEvents, maxElements);
        if (drainedElements < maxElements) {
            long currentTime = System.currentTimeMillis();
            while (currentTime < end && spanEvents.size() < maxElements) {
                try {
                    SpanEvent spanEvent = queue.poll(Math.max(end - currentTime, 0), TimeUnit.MILLISECONDS);
                    if (spanEvent != null) {
                        spanEvents.add(spanEvent);
                    }
                    currentTime = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while polling for spans.");
                }
            }
        }
        return spanEvents;
    }

}