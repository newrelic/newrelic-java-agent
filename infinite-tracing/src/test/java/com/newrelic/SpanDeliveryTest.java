package com.newrelic;

import com.newrelic.agent.interfaces.backport.Supplier;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpanDeliveryTest {

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    public BlockingQueue<SpanEvent> incomingQueue = new LinkedBlockingQueue<>();

    @Mock
    public SpanConverter<V1.Span> spanConverter;
    @Mock
    public MetricAggregator metricAggregator;
    @Mock
    public Logger logger;
    @Mock
    public Supplier<ClientCallStreamObserver<V1.Span>> streamObserverSupplier;

    @SuppressWarnings("unchecked")
    public ClientCallStreamObserver<V1.Span> mockStreamObserver() {
        return (ClientCallStreamObserver<V1.Span>) mock(ClientCallStreamObserver.class);
    }

    @Test
    public void noCallsIfStreamObserverNull() {
        SpanDelivery target = new SpanDelivery(
                spanConverter,
                metricAggregator,
                logger,
                incomingQueue,
                streamObserverSupplier);

        when(streamObserverSupplier.get()).thenReturn(null);

        incomingQueue.add(SpanEvent.builder().build());
        when(spanConverter.convert(any(SpanEvent.class))).thenAnswer(new AlwaysNewSpan());

        target.run();
        verifyNoInteractions(spanConverter, metricAggregator, logger);
        assertEquals(1, incomingQueue.size());
    }

    @Test
    public void returnsIfStreamObserverNotReady() {
        SpanDelivery target = new SpanDelivery(
                spanConverter,
                metricAggregator,
                logger,
                incomingQueue,
                streamObserverSupplier);

        incomingQueue.add(SpanEvent.builder().build());
        when(spanConverter.convert(any(SpanEvent.class))).thenAnswer(new AlwaysNewSpan());

        ClientCallStreamObserver<V1.Span> mockObserver = mockStreamObserver();
        when(streamObserverSupplier.get()).thenReturn(mockObserver);
        when(mockObserver.isReady()).thenReturn(false);
        target.run();

        verify(mockObserver, times(1)).isReady();
        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/NotReady");
        verifyNoInteractions(spanConverter, logger);
    }

    @Test
    public void doesNotCallOnNextIfQueueEmpty() {
        SpanDelivery target = new SpanDelivery(
                spanConverter,
                metricAggregator,
                logger,
                incomingQueue,
                streamObserverSupplier);

        when(spanConverter.convert(any(SpanEvent.class))).thenThrow(new AssertionError("should not convert"));

        ClientCallStreamObserver<V1.Span> mockObserver = mockStreamObserver();
        when(streamObserverSupplier.get()).thenReturn(mockObserver);
        when(mockObserver.isReady()).thenReturn(true);
        target.run();

        verify(mockObserver, times(1)).isReady();
        verify(mockObserver, never()).onNext(any(V1.Span.class));
    }

    @Test
    public void checksStreamObserverReadyAndCallsOnNext() {
        SpanDelivery target = new SpanDelivery(
                spanConverter,
                metricAggregator,
                logger,
                incomingQueue,
                streamObserverSupplier);

        incomingQueue.add(SpanEvent.builder().build());
        V1.Span mockSpan = mock(V1.Span.class);
        when(spanConverter.convert(any(SpanEvent.class))).thenReturn(mockSpan);

        ClientCallStreamObserver<V1.Span> mockObserver = mockStreamObserver();
        when(streamObserverSupplier.get()).thenReturn(mockObserver);
        when(mockObserver.isReady()).thenReturn(true);
        target.run();

        verify(mockObserver, times(1)).isReady();
        verify(mockObserver, times(1)).onNext(mockSpan);
    }

    @Test
    public void doesNotIncrementSentIfOnNextThrows() {
        final SpanDelivery target = new SpanDelivery(
                spanConverter,
                metricAggregator,
                logger,
                incomingQueue,
                streamObserverSupplier);

        incomingQueue.add(SpanEvent.builder().build());
        V1.Span mockSpan = mock(V1.Span.class);
        when(spanConverter.convert(any(SpanEvent.class))).thenReturn(mockSpan);

        ClientCallStreamObserver<V1.Span> mockObserver = mockStreamObserver();
        when(streamObserverSupplier.get()).thenReturn(mockObserver);
        when(mockObserver.isReady()).thenReturn(true);
        doThrow(new RuntimeException("~~ oops ~~")).when(mockObserver).onNext(any(V1.Span.class));

        assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() {
                target.run();
            }
        });
        verifyNoInteractions(metricAggregator);
    }

    @Test
    public void incrementsMetricOnSuccess() {
        SpanDelivery target = new SpanDelivery(
                spanConverter,
                metricAggregator,
                logger,
                incomingQueue,
                streamObserverSupplier);

        incomingQueue.add(SpanEvent.builder().build());
        V1.Span mockSpan = mock(V1.Span.class);
        when(spanConverter.convert(any(SpanEvent.class))).thenReturn(mockSpan);

        ClientCallStreamObserver<V1.Span> mockObserver = mockStreamObserver();
        when(streamObserverSupplier.get()).thenReturn(mockObserver);
        when(mockObserver.isReady()).thenReturn(true);
        target.run();

        verify(metricAggregator, times(1)).incrementCounter(anyString());
    }


    private static class AlwaysNewSpan implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocation) {
            return V1.Span.newBuilder().build();
        }
    }
}