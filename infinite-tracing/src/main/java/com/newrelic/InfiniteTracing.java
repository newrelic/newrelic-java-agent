package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;

import javax.annotation.concurrent.GuardedBy;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;

public class InfiniteTracing implements Consumer<SpanEvent> {

    private final Logger logger;
    private final InfiniteTracingConfig config;
    private final MetricAggregator aggregator;
    private final ExecutorService executorService;
    private final BlockingQueue<SpanEvent> queue;

    private final Object lock = new Object();
    @GuardedBy("lock") private Future<?> spanEventSenderFuture;
    @GuardedBy("lock") private SpanEventSender spanEventSender;
    @GuardedBy("lock") private ChannelManager channelManager;

    @VisibleForTesting
    InfiniteTracing(InfiniteTracingConfig config, MetricAggregator aggregator, ExecutorService executorService, BlockingQueue<SpanEvent> queue) {
        this.logger = config.getLogger();
        this.config = config;
        this.aggregator = aggregator;
        this.executorService = executorService;
        this.queue = queue;
    }

    /**
     * Start sending spans to the Infinite Tracing Observer. If already running, update with the
     * {@code agentRunToken} and {@code requestMetadata}.
     *
     * @param agentRunToken the agent run token
     * @param requestMetadata any extra metadata headers that must be included
     */
    public void start(String agentRunToken, Map<String, String> requestMetadata) {
        synchronized (lock) {
            // Record supportability metrics related to Infinite Tracing configuration settings
            aggregator.incrementCounter("Supportability/InfiniteTracing/gRPC/Compression/" + (config.getUseCompression() ? "enabled" : "disabled"));
            aggregator.incrementCounter("Supportability/InfiniteTracing/gRPC/Batching/" + (config.getUseBatching() ? "enabled" : "disabled"));

            if (spanEventSenderFuture != null) {
                channelManager.updateMetadata(agentRunToken, requestMetadata);
                channelManager.shutdownChannelAndBackoff(0);
                return;
            }
            logger.log(Level.INFO, "Starting Infinite Tracing.");
            channelManager = buildChannelManager(agentRunToken, requestMetadata);
            spanEventSender = buildSpanEventSender();
            spanEventSenderFuture = executorService.submit(spanEventSender);
        }
    }

    @VisibleForTesting
    ChannelManager buildChannelManager(String agentRunToken, Map<String, String> requestMetadata) {
        return new ChannelManager(config, aggregator, agentRunToken, requestMetadata);
    }

    @VisibleForTesting
    SpanEventSender buildSpanEventSender() {
        return new SpanEventSender(config, queue, aggregator, channelManager);
    }

    /**
     * Stop sending spans to the Infinite Tracing Observer and cleanup resources. If not already running,
     * return immediately.
     */
    public void stop() {
        synchronized (lock) {
            if (spanEventSenderFuture == null) {
                return;
            }
            logger.log(Level.INFO, "Stopping Infinite Tracing.");
            spanEventSenderFuture.cancel(true);
            channelManager.shutdownChannelForever();
            spanEventSenderFuture = null;
            spanEventSender = null;
            channelManager = null;
        }
    }

    /**
     * Offer the span event to the queue to be written to the Infinite Trace Observer. If the queue
     * is at capacity, the span event is ignored.
     *
     * @param spanEvent the span event
     */
    @Override
    public void accept(SpanEvent spanEvent) {
        aggregator.incrementCounter("Supportability/InfiniteTracing/Span/Seen");
        if (!queue.offer(spanEvent)) {
            logger.log(Level.FINEST, "Span event not accepted. The queue was full.");
        }
    }

    /**
     * Initialize Infinite Tracing. Note, for spans to start being sent {@link #start(String, Map)} must
     * be called.
     *
     * @param config the config
     * @param aggregator the metric aggregator
     * @return the instance
     */
    public static InfiniteTracing initialize(InfiniteTracingConfig config, MetricAggregator aggregator) {
        ExecutorService executorService = Executors.newSingleThreadExecutor(new DaemonThreadFactory("Infinite Tracing"));
        return new InfiniteTracing(config, aggregator, executorService, new LinkedBlockingDeque<SpanEvent>(config.getMaxQueueSize()));
    }

    static class DaemonThreadFactory implements ThreadFactory {
        private final String serviceName;
        private final AtomicInteger counter = new AtomicInteger(0);

        DaemonThreadFactory(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("New Relic " + serviceName + " #" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

}
