package com.newrelic;

import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class ResponseObserver implements StreamObserver<V1.RecordStatus> {
    private final MetricAggregator metricAggregator;
    private final Logger logger;
    private final DisconnectionHandler disconnectionHandler;
    private final AtomicBoolean shouldRecreateCall;

    public ResponseObserver(MetricAggregator metricAggregator, Logger logger, DisconnectionHandler disconnectionHandler,
                            AtomicBoolean shouldRecreateCall) {
        this.metricAggregator = metricAggregator;
        this.logger = logger;
        this.disconnectionHandler = disconnectionHandler;
        this.shouldRecreateCall = shouldRecreateCall;
    }

    @Override
    public void onNext(V1.RecordStatus value) {
        metricAggregator.incrementCounter("Supportability/InfiniteTracing/Response");
    }

    @Override
    public void onError(Throwable t) {
        if (isChannelClosing(t)) {
            logger.log(Level.FINE, "Stopping current gRPC call because the channel is closing.");
            return;
        }

        if (isAlpnError(t)) {
            logger.log(Level.SEVERE, t, "ALPN does not appear to be supported on this JVM. Please install a supported JCE provider or update Java to use Infinite Tracing");
            metricAggregator.incrementCounter("Supportability/InfiniteTracing/NoALPNSupport");
            disconnectionHandler.terminate();
            return;
        }

        if (!isConnectionTimeoutException(t)) {
            logger.log(Level.WARNING, t, "Encountered gRPC exception");
        }

        metricAggregator.incrementCounter("Supportability/InfiniteTracing/Response/Error");

        Status status = null;
        if (t instanceof StatusRuntimeException) {
            StatusRuntimeException statusRuntimeException = (StatusRuntimeException) t;
            status = statusRuntimeException.getStatus();
            metricAggregator.incrementCounter("Supportability/InfiniteTracing/Span/gRPC/" + status.getCode());
        }

        disconnectionHandler.handle(status);
    }

    @Override
    public void onCompleted() {
        logger.log(Level.FINE, "Completing gRPC call.");
        shouldRecreateCall.set(true);
        metricAggregator.incrementCounter("Supportability/InfiniteTracing/Response/Completed");
        disconnectionHandler.resetConnectBackoffPolicy();
    }

    /**
     * Detects if the error received was another thread knowingly cancelling the gRPC call.
     */
    private boolean isChannelClosing(Throwable t) {
        return t instanceof StatusRuntimeException && t.getCause() instanceof ChannelClosingException;
    }

    /**
     * Detects if the error received was a connection timeout exception. This can happen if the agent hasn't sent any spans for more than 15 seconds.
     */
    private boolean isConnectionTimeoutException(Throwable t) {
        return t instanceof StatusRuntimeException
                && t.getMessage().startsWith("INTERNAL: No error: A GRPC status of OK should have been sent");
    }

    /**
     * Attempts to detect if the error received indicates that ALPN support is not provided by this JVM.
     */
    private boolean isAlpnError(Throwable t) {
        return t instanceof StatusRuntimeException
                && t.getCause() instanceof RuntimeException
                && isOkHttpALPNException((RuntimeException) t.getCause());
    }

    private boolean isOkHttpALPNException(RuntimeException cause) {
        // See https://github.com/grpc/grpc-java/blob/v1.28.1/okhttp/src/main/java/io/grpc/okhttp/OkHttpProtocolNegotiator.java#L96
        // It's the only mechanism we have to identify the problem.
        return cause.getMessage() != null && cause.getMessage().startsWith("TLS ALPN negotiation failed with protocols");
    }
}
