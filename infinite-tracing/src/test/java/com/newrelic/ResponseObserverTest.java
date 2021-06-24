package com.newrelic;

import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Level;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResponseObserverTest {

    @Mock
    private Logger logger;
    @Mock
    private ChannelManager channelManager;
    @Mock
    private MetricAggregator aggregator;
    @Mock
    private BackoffPolicy backoffPolicy;

    private ResponseObserver target;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        target = spy(new ResponseObserver(logger, channelManager, aggregator, backoffPolicy));
    }

    @Test
    void onNext_IncrementsCounter() {
        target.onNext(V1.RecordStatus.newBuilder().build());

        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/Response");
        verify(backoffPolicy).reset();
    }

    @Test
    void onError_ChannelClosingExceptionReturns() {
        StatusRuntimeException exception = Status.CANCELLED.withCause(new ChannelClosingException()).asRuntimeException();

        target.onError(exception);

        verifyNoInteractions(channelManager, aggregator);
    }

    @Test
    void onError_AlpnErrorShutdownChannelForever() {
        RuntimeException cause = new RuntimeException("TLS ALPN negotiation failed with protocols: [h2]");
        StatusRuntimeException exception = Status.UNAVAILABLE.withCause(cause).asRuntimeException();

        target.onError(exception);

        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/NoALPNSupport");
        verify(channelManager).shutdownChannelForever();
    }

    @Test
    void onError_UnimplementedShutdownChannelForever() {
        target.onError(Status.UNIMPLEMENTED.asException());

        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/Span/gRPC/" + Status.UNIMPLEMENTED.getCode());
        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/Response/Error");
        verify(channelManager).shutdownChannelForever();
    }

    @Test
    void onError_OtherStatusShutdownChannelAndBackoff() {
        doNothing().when(target).shutdownChannelAndBackoff(ArgumentMatchers.<Status>any());

        target.onError(Status.INTERNAL.asException());
        target.onError(Status.FAILED_PRECONDITION.asException());
        target.onError(Status.UNKNOWN.asException());

        verify(aggregator, times(6)).incrementCounter(anyString());
        verify(target, times(3)).shutdownChannelAndBackoff(ArgumentMatchers.<Status>any());
    }

    @Test
    void shutdownChannelAndBackoff_ConnectTimeoutBackoffZeroSeconds() {
        Status status = Status.fromCode(Status.Code.INTERNAL).withDescription("No error: A GRPC status of OK should have been sent\nRst Stream");

        target.shutdownChannelAndBackoff(status);

        verify(logger).log(eq(Level.FINE), any(Throwable.class), anyString(), anyString());
        verify(channelManager).shutdownChannelAndBackoff(0);
    }

    @Test
    void shutdownChannelAndBackoff_FailedPreconditionBackoffSequence() {
        int backoffSeconds = 5;
        when(backoffPolicy.getNextBackoffSeconds()).thenReturn(backoffSeconds);

        target.shutdownChannelAndBackoff(Status.FAILED_PRECONDITION);

        verify(logger).log(eq(Level.WARNING), any(Throwable.class), anyString(), anyString());
        verify(channelManager, atLeast(1)).shutdownChannelAndBackoff(backoffSeconds);
    }

    @Test
    void shutdownChannelAndBackoff_OtherStatusDefaultBackoff() {
        int backoffSeconds = 5;
        when(backoffPolicy.getDefaultBackoffSeconds()).thenReturn(backoffSeconds);

        target.shutdownChannelAndBackoff(Status.UNKNOWN);

        verify(logger).log(eq(Level.WARNING), any(Throwable.class), anyString(), anyString());
        verify(channelManager, atLeast(1)).shutdownChannelAndBackoff(backoffSeconds);
    }

    @Test
    void onCompleted_IncrementsCounterCancelsSpanObserver() {
        target.onCompleted();

        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/Response/Completed");
        verify(channelManager).recreateSpanObserver();
    }

}