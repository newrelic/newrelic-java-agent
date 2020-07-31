package com.newrelic;

import com.newrelic.agent.interfaces.backport.Supplier;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

class SpanDelivery implements Runnable {

    private final SpanConverter<V1.Span> spanConverter;
    private final MetricAggregator metricAggregator;
    private final Logger logger;
    private final BlockingQueue<SpanEvent> queue;
    private final Supplier<ClientCallStreamObserver<V1.Span>> streamObserverSupplier;

    public SpanDelivery(SpanConverter<V1.Span> spanConverter, MetricAggregator metricAggregator, Logger logger, BlockingQueue<SpanEvent> queue,
            Supplier<ClientCallStreamObserver<V1.Span>> streamObserverSupplier) {
        this.spanConverter = spanConverter;
        this.metricAggregator = metricAggregator;
        this.logger = logger;
        this.queue = queue;
        this.streamObserverSupplier = streamObserverSupplier;
    }

    @Override
    public void run() {
        ClientCallStreamObserver<V1.Span> spanClientCallStreamObserver = streamObserverSupplier.get();

        if (spanClientCallStreamObserver == null) {
            return;
        }

        if (!spanClientCallStreamObserver.isReady()) {
            try {
                metricAggregator.incrementCounter("Supportability/InfiniteTracing/NotReady");
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        SpanEvent spanEvent = pollSafely();
        if (spanEvent == null) {
            return;
        }

        V1.Span outputSpan = spanConverter.convert(spanEvent);

        try {
            spanClientCallStreamObserver.onNext(outputSpan);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, t, "Unable to send span!");
            throw t;
        }

        metricAggregator.incrementCounter("Supportability/InfiniteTracing/Span/Sent");
    }

    private SpanEvent pollSafely() {
        try {
            return queue.poll(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Thread was interrupted while polling for spans.");
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
