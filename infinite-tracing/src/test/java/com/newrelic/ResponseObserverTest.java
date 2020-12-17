package com.newrelic;

import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ResponseObserverTest {

    AtomicBoolean shouldRecreateCall = new AtomicBoolean();

    @Mock
    public MetricAggregator metricAggregator;

    @Mock
    public Logger logger;

    @Mock
    public DisconnectionHandler disconnectionHandler;

    public ResponseObserver target;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        target = new ResponseObserver(
                metricAggregator,
                logger,
                disconnectionHandler, shouldRecreateCall);
    }

    @Test
    public void shouldIncrementCounterOnNext() {
        target.onNext(V1.RecordStatus.newBuilder().setMessagesSeen(3000).build());

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Response");
    }

    @Test
    public void shouldDisconnectOnNormalException() {
        target.onError(new Throwable());

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Response/Error");
        verify(disconnectionHandler).handle(null);
    }

    @Test
    public void shouldReportStatusOnError() {
        StatusRuntimeException exception = new StatusRuntimeException(Status.CANCELLED);

        target.onError(exception);

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Span/gRPC/CANCELLED");
        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Response/Error");
        verify(disconnectionHandler).handle(Status.CANCELLED);
    }

    @Test
    public void shouldNotDisconnectWhenChannelClosing() {
        StatusRuntimeException exception = Status.CANCELLED.withCause(new ChannelClosingException()).asRuntimeException();

        target.onError(exception);

        verifyNoInteractions(disconnectionHandler, metricAggregator);
    }

    @Test
    public void shouldDisconnectOnCompleted() {
        target.onCompleted();

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Response/Completed");
        assertTrue(shouldRecreateCall.get());
    }

    @Test
    public void shouldTerminateOnALPNError() {
        RuntimeException cause = new RuntimeException("TLS ALPN negotiation failed with protocols: [h2]");
        StatusRuntimeException exception = Status.UNAVAILABLE.withCause(cause).asRuntimeException();

        target.onError(exception);

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/NoALPNSupport");
        verify(disconnectionHandler).terminate();
    }

    @Test
    public void testIsConnectionTimeoutException() {
        Throwable exception = new StatusRuntimeException(
                Status.fromCode(Status.Code.INTERNAL).withDescription("No error: A GRPC status of OK should have been sent\nRst Stream"));
        target.onError(exception);

        verify(logger, never()).log(Level.WARNING, exception, "Encountered gRPC exception");
    }

    @Test
    public void testConnectionTimeoutExceptionWrongType() {
        Throwable exception = new RuntimeException("No error: A GRPC status of OK should have been sent\nRst Stream");
        target.onError(exception);

        verify(logger).log(Level.WARNING, exception, "Encountered gRPC exception");
    }

    @Test
    public void testConnectionTimeoutExceptionWrongMessage() {
        Throwable exception = new StatusRuntimeException(Status.fromCode(Status.Code.INTERNAL).withDescription("A REALLY BAD ERROR: PRINT ME"));
        target.onError(exception);

        verify(logger).log(Level.WARNING, exception, "Encountered gRPC exception");
    }

}