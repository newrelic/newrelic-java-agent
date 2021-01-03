package com.newrelic;

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
    private final String agentRunToken;
    private final Map<String, String> requestMetadata;
    private final InfiniteTracingConfig config;
    private final MetricAggregator aggregator;

    private final Object lock = new Object();
    @GuardedBy("lock") private boolean isShutdownForever;
    @GuardedBy("lock") private CountDownLatch backoffLatch;
    @GuardedBy("lock") private ManagedChannel managedChannel;
    @GuardedBy("lock") private ClientCallStreamObserver<V1.Span> spanObserver;
    @GuardedBy("lock") private ResponseObserver responseObserver;

    ChannelManager(InfiniteTracingConfig config, String agentRunToken, Map<String, String> requestMetadata, MetricAggregator aggregator) {
        this.logger = config.getLogger();
        this.config = config;
        this.agentRunToken = agentRunToken;
        this.requestMetadata = requestMetadata;
        this.aggregator = aggregator;
    }

    /**
     * Obtain a span observer. Creates a channel if one is not open. Creates a span observer if one
     * does not exist. If the channel has been shutdown and is backing off via
     * {@link #shutdownChannelAndBackoff(int)}, awaits the backoff period before recreating the channel.
     *
     * @return a span observer
     */
    ClientCallStreamObserver<V1.Span> getSpanObserver() {
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
            if (spanObserver == null) {
                logger.log(Level.FINE, "Creating gRPC span observer.");
                IngestServiceStub ingestServiceStub = IngestServiceGrpc.newStub(managedChannel);
                responseObserver = new ResponseObserver(logger, this, aggregator);
                spanObserver = (ClientCallStreamObserver<V1.Span>) ingestServiceStub.recordSpan(responseObserver);
                aggregator.incrementCounter("Supportability/InfiniteTracing/Connect");
            }
            return spanObserver;
        }
    }

    /**
     * Cancel the span observer. The next time {@link #getSpanObserver()} is called the span observer
     * will be recreated. This cancels the span observer with a {@link ChannelClosingException}, which
     * {@link ResponseObserver#onError(Throwable)} detects and ignores.
     */
    void cancelSpanObserver() {
        synchronized (lock) {
            logger.log(Level.FINE, "Canceling gRPC span observer.");
            spanObserver.cancel("CLOSING_CONNECTION", new ChannelClosingException());
            spanObserver = null;
            responseObserver = null;
        }
    }

    /**
     * Shutdown the channel, cancel the span observer, and backoff. The next time {@link #getSpanObserver()}
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
            cancelSpanObserver();
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
     * Shutdown the channel and do not recreate it. The next time {@link #getSpanObserver()} is called
     * an exception will be thrown.
     */
    void shutdownChannelForever() {
        synchronized (lock) {
            logger.log(Level.FINE, "Shutting down gRPC channel forever.");
            shutdownChannelAndBackoff(0);
            this.isShutdownForever = true;
        }
    }

    private ManagedChannel buildChannel() {
        Map<String, String> headers = new HashMap<>(requestMetadata);
        headers.put("agent_run_token", agentRunToken);
        headers.put("license_key", config.getLicenseKey());
        if (config.getFlakyPercentage() != null) {
            logger.log(Level.WARNING, "Infinite tracing is configured with a flaky percentage! There will be errors!");
            headers.put("flaky", config.getFlakyPercentage().toString());
        }

        OkHttpChannelBuilder channelBuilder = OkHttpChannelBuilder
                .forAddress(config.getHost(), config.getPort())
                .defaultLoadBalancingPolicy("pick_first")
                .keepAliveTime(5, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .intercept(new HeadersInterceptor(headers));
        if (config.getUsePlaintext()) {
            channelBuilder.usePlaintext();
        } else {
            channelBuilder.useTransportSecurity();
        }
        return channelBuilder.build();
    }

}