package com.newrelic;

import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.exceptions.verification.WantedButNotInvoked;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpanEventConsumerTest {
    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    @Mock
    public ChannelFactory mockChannelFactory;
    @Mock
    public ManagedChannel mockChannel;
    @Mock
    public StreamObserverFactory mockStreamObserverFactory;
    @Mock
    public ClientCallStreamObserver<V1.Span> mockStreamObserver;
    @Mock
    public Logger mockLogger;
    @Mock
    public MetricAggregator metricAggregator;

    @Test
    @Timeout(30)
    public void integrationTest() throws InterruptedException {
        when(mockChannelFactory.createChannel()).thenReturn(mockChannel);
        when(mockStreamObserverFactory.buildStreamObserver(mockChannel)).thenReturn(mockStreamObserver);
        when(mockStreamObserver.isReady()).thenReturn(true);

        InfiniteTracingConfig config = InfiniteTracingConfig.builder()
                .logger(mockLogger)
                .maxQueueSize(10)
                .build();

        SpanEventConsumer target = SpanEventConsumer.builder(config, metricAggregator)
                .setChannelFactory(mockChannelFactory)
                .setStreamObserverFactory(mockStreamObserverFactory)
                .build();

        target.start();

        SpanEvent incomingEvent = SpanEvent.builder()
                .putIntrinsic("traceId", "123abc")
                .putIntrinsic("guid", "some-intrinsic")
                .appName("app-name")
                .build();

        target.accept(incomingEvent);

        while (true) {
            ArgumentCaptor<V1.Span> outgoingSpanCaptor = ArgumentCaptor.forClass(V1.Span.class);
            try {
                verify(mockStreamObserver, times(1)).onNext(outgoingSpanCaptor.capture());
            } catch (WantedButNotInvoked ignored) {
                Thread.sleep(10);
                continue;
            }

            V1.Span capturedSpan = outgoingSpanCaptor.getValue();

            assertEquals("123abc", capturedSpan.getTraceId());
            assertEquals("some-intrinsic", capturedSpan.getIntrinsicsOrThrow("guid").getStringValue());
            assertEquals("app-name", capturedSpan.getIntrinsicsOrThrow("appName").getStringValue());
            break;
        }

    }

}