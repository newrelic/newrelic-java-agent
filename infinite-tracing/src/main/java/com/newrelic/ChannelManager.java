package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.IngestServiceGrpc;
import com.newrelic.trace.v1.IngestServiceGrpc.IngestServiceStub;
import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;

import javax.annotation.concurrent.GuardedBy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

class ChannelManager {

    private final Logger logger;
    private final InfiniteTracingConfig config;
    private final MetricAggregator aggregator;
    private final BackoffPolicy backoffManager;

    private final Object lock = new Object();
    @GuardedBy("lock") private boolean isShutdownForever;
    @GuardedBy("lock") private CountDownLatch backoffLatch;
    @GuardedBy("lock") private ManagedChannel managedChannel;
    @GuardedBy("lock") private boolean recreateSpanObserver = true;
    @GuardedBy("lock") private Observer observer;
    @GuardedBy("lock") private String agentRunToken;
    @GuardedBy("lock") private Map<String, String> requestMetadata;

    ChannelManager(InfiniteTracingConfig config, MetricAggregator aggregator, String agentRunToken, Map<String, String> requestMetadata) {
        this.logger = config.getLogger();
        this.config = config;
        this.aggregator = aggregator;
        this.agentRunToken = agentRunToken;
        this.requestMetadata = requestMetadata;
        this.backoffManager = new BackoffPolicy();
    }

    /**
     * Update metadata included on gRPC requests.
     *
     * @param agentRunToken the agent run token
     * @param requestMetadata any extra metadata headers that must be included
     */
    void updateMetadata(String agentRunToken, Map<String, String> requestMetadata) {
        synchronized (lock) {
            this.agentRunToken = agentRunToken;
            this.requestMetadata = requestMetadata;
        }
    }

    /**
     * Obtain a span observer. Creates a channel if one is not open. Creates a span observer if one
     * does not exist. If the channel has been shutdown and is backing off via
     * {@link #shutdownChannelAndBackoff(int)}, awaits the backoff period before recreating the channel.
     *
     * @return a span observer
     */
    Observer getObserver() {
        // Obtain the lock, and await the backoff if in progress
        CountDownLatch latch;
        synchronized (lock) {
            latch = backoffLatch;
        }
        if (latch != null) {
            try {
                logger.log(Level.FINE, "Awaiting backoff.");
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while awaiting backoff.");
            }
        }

        // Obtain the lock, and possibly recreate the channel or the span observer
        synchronized (lock) {
            if (isShutdownForever) {
                throw new RuntimeException("No longer accepting connections to gRPC.");
            }
            if (managedChannel == null) {
                logger.log(Level.FINE, "Creating gRPC channel.");
                managedChannel = buildChannel();
            }
            if (recreateSpanObserver) {
                if (observer != null) {
                    logger.log(Level.FINE, "Cancelling and recreating gRPC span observer.");
                    observer.cancel("CLOSING_CONNECTION", new ChannelClosingException());
                }
                IngestServiceStub ingestServiceStub = buildStub(managedChannel);
                ResponseObserver responseObserver = buildResponseObserver();
                if (config.getUseBatching()) {
                    observer = buildSpanBatchObserver((ClientCallStreamObserver<V1.SpanBatch>) ingestServiceStub.recordSpanBatch(responseObserver));
                } else {
                    observer = buildSpanObserver((ClientCallStreamObserver<V1.Span>) ingestServiceStub.recordSpan(responseObserver));
                }
                aggregator.incrementCounter("Supportability/InfiniteTracing/Connect");
                recreateSpanObserver = false;
            }
            return observer;
        }
    }

    @VisibleForTesting
    Observer buildSpanObserver(ClientCallStreamObserver<V1.Span> observer) {
        return new SpanObserver(observer);
    }

    @VisibleForTesting
    Observer buildSpanBatchObserver(ClientCallStreamObserver<V1.SpanBatch> observer) {
        return new SpanBatchObserver(observer);
    }

    @VisibleForTesting
    IngestServiceStub buildStub(ManagedChannel managedChannel) {
        IngestServiceStub ingestServiceStub = IngestServiceGrpc.newStub(managedChannel);
        if (config.getUseCompression()) {
            ingestServiceStub = ingestServiceStub.withCompression("gzip");
        }
        return ingestServiceStub;
    }

    @VisibleForTesting
    ResponseObserver buildResponseObserver() {
        return new ResponseObserver(logger, this, aggregator, backoffManager);
    }

    /**
     * Mark that the span observer should be canceled and recreated the next time {@link #getObserver()} is called.
     */
    void recreateSpanObserver() {
        synchronized (lock) {
            recreateSpanObserver = true;
        }
    }

    /**
     * Shutdown the channel, cancel the span observer, and backoff. The next time {@link #getObserver()}
     * is called, it will await the backoff and the channel will be recreated.
     *
     * @param backoffSeconds the number of seconds to await before the channel can be recreated
     */
    void shutdownChannelAndBackoff(int backoffSeconds) {
        logger.log(Level.FINE, "Shutting down gRPC channel and backing off for {0} seconds.", backoffSeconds);
        CountDownLatch latch;
        synchronized (lock) {
            if (backoffLatch != null) {
                logger.log(Level.FINE, "Backoff already in progress.");
                return;
            }
            backoffLatch = new CountDownLatch(1);
            latch = backoffLatch;

            if (managedChannel != null) {
                managedChannel.shutdown();
                managedChannel = null;
            }
            recreateSpanObserver();
        }

        try {
            TimeUnit.SECONDS.sleep(backoffSeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while backing off.");
        }

        synchronized (lock) {
            latch.countDown();
            backoffLatch = null;
        }
        logger.log(Level.FINE, "Backoff complete.");
    }

    /**
     * Shutdown the channel and do not recreate it. The next time {@link #getObserver()} is called
     * an exception will be thrown.
     */
    void shutdownChannelForever() {
        synchronized (lock) {
            logger.log(Level.FINE, "Shutting down gRPC channel forever.");
            shutdownChannelAndBackoff(0);
            this.isShutdownForever = true;
        }
    }

    @VisibleForTesting
    ManagedChannel buildChannel() {
        Map<String, String> headers;
        synchronized (lock) {
            headers = requestMetadata != null ? new HashMap<>(requestMetadata) : new HashMap<String, String>();
            headers.put("agent_run_token", agentRunToken);
        }

        headers.put("license_key", config.getLicenseKey());
        if (config.getFlakyPercentage() != null) {
            logger.log(Level.WARNING, "Infinite tracing is configured with a flaky percentage! There will be errors!");
            headers.put("flaky", config.getFlakyPercentage().toString());
            if (config.getFlakyCode() != null) {
                headers.put("flaky_code", config.getFlakyCode().toString());
            }
        }

        OkHttpChannelBuilder channelBuilder = OkHttpChannelBuilder
                .forAddress(config.getHost(), config.getPort())
                .defaultLoadBalancingPolicy("pick_first")
                .intercept(new HeadersInterceptor(headers));
        if (config.getUsePlaintext()) {
            channelBuilder.usePlaintext();
        } else {
            channelBuilder.useTransportSecurity();
        }
        return channelBuilder.build();
    }

}
