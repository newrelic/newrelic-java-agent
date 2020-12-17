package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.agent.interfaces.backport.Supplier;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Accepts a {@link SpanEvent} for publishing to the Trace Observer.
 *
 * Not thread-safe.
 */
public class SpanEventConsumer implements Consumer<SpanEvent> {
    private final Logger logger;
    private final BlockingQueue<SpanEvent> queue;
    private final MetricAggregator aggregator;
    private final ConnectionHeaders connectionHeaders;
    private final Runnable spanSender;
    private final ExecutorService executorService;
    private final AtomicBoolean wasStarted = new AtomicBoolean(false);
    private volatile Future<?> senderFuture;

    private SpanEventConsumer(Logger logger, BlockingQueue<SpanEvent> queue, MetricAggregator aggregator, ConnectionHeaders connectionHeaders,
            Runnable spanSender, ExecutorService executorService) {
        this.logger = logger;
        this.queue = queue;
        this.aggregator = aggregator;
        this.connectionHeaders = connectionHeaders;
        this.spanSender = spanSender;
        this.executorService = executorService;
    }

    @Override
    public void accept(SpanEvent spanEvent) {
        aggregator.incrementCounter("Supportability/InfiniteTracing/Span/Seen");
            if(!queue.offer(spanEvent)) {
                logger.log(Level.FINEST, "Span event not accepted. The queue was full.");
            }
    }

    public static SpanEventConsumer.Builder builder(InfiniteTracingConfig config, MetricAggregator metricAggregator) {
        return new Builder(config, metricAggregator);
    }

    public void start() {
        if (wasStarted.compareAndSet(false, true)) {
            senderFuture = executorService.submit(spanSender);
        }
    }

    public void stop() {
        if (wasStarted.compareAndSet(true,false)) {
            senderFuture.cancel(true);
            senderFuture = null;
        }
    }

    public void setConnectionMetadata(String newRunToken, Map<String, String> headers) {
        connectionHeaders.set(newRunToken, headers);
    }

    public static class Builder {
        private final InfiniteTracingConfig config;
        private final SpanConverter<V1.Span> spanConverter = new GrpcSpanConverter();
        private final MetricAggregator metricAggregator;
        private final Logger logger;
        private final BlockingQueue<SpanEvent> queue;

        private ChannelFactory channelFactory;
        private StreamObserverFactory streamObserverFactory;

        public Builder(InfiniteTracingConfig config, MetricAggregator metricAggregator) {
            this.logger = config.getLogger();
            this.queue = new LinkedBlockingQueue<>(config.getMaxQueueSize());
            this.metricAggregator = metricAggregator;
            this.config = config;
        }

        @VisibleForTesting
        public Builder setChannelFactory(ChannelFactory channelFactory) {
            this.channelFactory = channelFactory;
            return this;
        }

        @VisibleForTesting
        public Builder setStreamObserverFactory(StreamObserverFactory streamObserverFactory) {
            this.streamObserverFactory = streamObserverFactory;
            return this;
        }

        public SpanEventConsumer build() {
            BackoffPolicy defaultBackoffPolicy = new DefaultBackoffPolicy();
            ConnectBackoffPolicy connectBackoffPolicy = new ConnectBackoffPolicy();
            ConnectionStatus connectionStatus = new ConnectionStatus(logger);

            ConnectionHeaders connectionHeaders = new ConnectionHeaders(connectionStatus, logger, config.getLicenseKey());
            ClientInterceptor clientInterceptor = new HeadersInterceptor(connectionHeaders);
            ClientInterceptor maybeInjectFlakyHeader = new FlakyHeaderInterceptor(config);
            DisconnectionHandler disconnectionHandler = new DisconnectionHandler(connectionStatus, defaultBackoffPolicy, connectBackoffPolicy, logger);
            AtomicBoolean shouldRecreateCall = new AtomicBoolean(false);

            ResponseObserver responseObserver = new ResponseObserver(metricAggregator, logger, disconnectionHandler, shouldRecreateCall);

            ChannelFactory channelFactory = this.channelFactory != null
                ? this.channelFactory
                : new ChannelFactory(config, clientInterceptor, maybeInjectFlakyHeader);

            StreamObserverFactory streamObserverFactory = this.streamObserverFactory != null
                ? this.streamObserverFactory
                : new StreamObserverFactory(logger, metricAggregator, responseObserver);

            Function<ManagedChannel, ClientCallStreamObserver<V1.Span>> channelToStreamObserverConverter =
                    new ChannelToStreamObserver(streamObserverFactory, shouldRecreateCall);

            Supplier<ManagedChannel> channelSupplier = new ChannelSupplier(connectionStatus, logger, channelFactory);

            Supplier<ClientCallStreamObserver<V1.Span>> streamObserverSupplier = new StreamObserverSupplier(channelSupplier, channelToStreamObserverConverter);

            Runnable spanDeliveryConsumer = new SpanDelivery(spanConverter, metricAggregator, logger, queue, streamObserverSupplier);

            Runnable loopForever = new LoopForever(logger, spanDeliveryConsumer);

            ExecutorService executorService = Executors.newSingleThreadExecutor(new DaemonThreadFactory("Span Event Consumer"));

            return new SpanEventConsumer(logger, queue, metricAggregator, connectionHeaders, loopForever, executorService);
        }
    }

}
