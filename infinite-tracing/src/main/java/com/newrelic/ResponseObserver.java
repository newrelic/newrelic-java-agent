package com.newrelic;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

class ResponseObserver implements StreamObserver<V1.RecordStatus> {

    static final int DEFAULT_BACKOFF_SECONDS = 15;
    static final int[] BACKOFF_SECONDS_SEQUENCE = new int[] { 15, 15, 30, 60, 120, 300 };

    private final Logger logger;
    private final ChannelManager channelManager;
    private final MetricAggregator aggregator;
    private final AtomicInteger backoffSequenceIndex = new AtomicInteger(-1);

    ResponseObserver(Logger logger, ChannelManager channelManager, MetricAggregator aggregator) {
        this.logger = logger;
        this.channelManager = channelManager;
        this.aggregator = aggregator;
    }

    @Override
    public void onNext(V1.RecordStatus value) {
        aggregator.incrementCounter("Supportability/InfiniteTracing/Response");
    }

    @Override
    public void onError(Throwable t) {
        Status status = Status.fromThrowable(t);

        // If the span observer is knowingly cancelled, it is canceled with a ChannelClosingException,
        // which is detected here and ignored.
        if (status.getCause() instanceof ChannelClosingException) {
            logger.log(Level.FINE, "Stopping gRPC response observer because span observer was closed by another thread.");
            return;
        }

        if (isOkHttpALPNError(status)) {
            logger.log(Level.SEVERE, t,
                    "ALPN does not appear to be supported on this JVM. Please install a supported JCE provider or update Java to use Infinite Tracing.");
            aggregator.incrementCounter("Supportability/InfiniteTracing/NoALPNSupport");
            channelManager.shutdownChannelForever();
            return;
        }

        aggregator.incrementCounter("Supportability/InfiniteTracing/Span/gRPC/" + status.getCode());
        aggregator.incrementCounter("Supportability/InfiniteTracing/Response/Error");

        if (status.getCode() == Status.Code.UNIMPLEMENTED) {
            // See: https://source.datanerd.us/agents/agent-specs/blob/master/Infinite-Tracing.md#unimplemented
            logger.log(Level.WARNING, "Received gRPC status {0}, no longer permitting connections.", status.getCode().toString());
            channelManager.shutdownChannelForever();
            return;
        }

        shutdownChannelAndBackoff(status);
    }

    /**
     * Determine if the error status indicates that ALPN support is not provided by this JVM.
     */
    private static boolean isOkHttpALPNError(Status status) {
        // See https://github.com/grpc/grpc-java/blob/v1.28.1/okhttp/src/main/java/io/grpc/okhttp/OkHttpProtocolNegotiator.java#L96
        // It's the only mechanism we have to identify the problem.
        return status.getCause() instanceof RuntimeException
                && status.getCause().getMessage().startsWith("TLS ALPN negotiation failed with protocols");
    }

    /**
     * Shutdown and backoff the gRPC channel. The amount of time to backoff before allowing reconnection
     * is dictated by the {@code status}.
     *
     * @param status the error status
     */
    @VisibleForTesting
    void shutdownChannelAndBackoff(Status status) {
        int backoffSeconds;
        Level logLevel = Level.WARNING;

        if (isConnectTimeoutError(status)) {
            backoffSeconds = 0;
            logLevel = Level.FINE;
        } else if (status.getCode() == Status.Code.FAILED_PRECONDITION) {
            // See: https://source.datanerd.us/agents/agent-specs/blob/master/Infinite-Tracing.md#failed_precondition
            int nextIndex = backoffSequenceIndex.incrementAndGet();
            backoffSeconds = nextIndex < BACKOFF_SECONDS_SEQUENCE.length
                    ? BACKOFF_SECONDS_SEQUENCE[nextIndex]
                    : BACKOFF_SECONDS_SEQUENCE[BACKOFF_SECONDS_SEQUENCE.length - 1];
        } else {
            // See: https://source.datanerd.us/agents/agent-specs/blob/master/Infinite-Tracing.md#other-errors-1
            backoffSeconds = DEFAULT_BACKOFF_SECONDS;
        }

        logger.log(logLevel, status.asException(), "Received gRPC status {0}.", status.getCode().toString());
        channelManager.shutdownChannelAndBackoff(backoffSeconds);
    }

    /**
     * Determine if the error status is a connection timeout exception. This can happen if the agent hasn't sent
     * any spans for more than 15 seconds.
     */
    private static boolean isConnectTimeoutError(Status status) {
        return status.getCode() == Status.Code.INTERNAL
                && status.getDescription() != null
                && status.getDescription().startsWith("No error: A GRPC status of OK should have been sent");
    }

    @Override
    public void onCompleted() {
        logger.log(Level.FINE, "Completing gRPC response observer.");
        aggregator.incrementCounter("Supportability/InfiniteTracing/Response/Completed");
        channelManager.cancelSpanObserver();
    }

}