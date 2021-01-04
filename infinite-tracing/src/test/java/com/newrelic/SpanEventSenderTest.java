package com.newrelic;

import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.newrelic.SpanEventSender.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpanEventSenderTest {

    @Mock
    private Logger logger;
    @Mock
    private InfiniteTracingConfig config;
    @Mock
    private BlockingQueue<SpanEvent> queue;
    @Mock
    private MetricAggregator aggregator;
    @Mock
    private ChannelManager channelManager;
    @Mock
    private ClientCallStreamObserver<V1.Span> observer;

    private SpanEventSender target;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        when(config.getLogger()).thenReturn(logger);
        when(channelManager.getSpanObserver()).thenReturn(observer);
        target = spy(new SpanEventSender(config, queue, aggregator, channelManager));
    }

    @Test
    @Timeout(5)
    void run_CallsPollAndWriteUntilException() {
        doNothing()
                .doNothing()
                .doThrow(new RuntimeException("Error!"))
                .when(target).pollAndWrite();

        target.run();
        verify(target, times(3)).pollAndWrite();
    }

    @Test
    void pollAndWrite_ObserverNotReadyDoesNotPoll() {
        doReturn(false).when(target).awaitReadyObserver(observer);

        target.pollAndWrite();

        verify(target, never()).pollSafely();
        verify(target, never()).writeToObserver(ArgumentMatchers.<ClientCallStreamObserver<V1.Span>>any(), ArgumentMatchers.<V1.Span>any());
    }

    @Test
    void pollAndWrite_NullSpanDoesNotWrite() {
        doReturn(true).when(target).awaitReadyObserver(observer);
        doReturn(null).when(target).pollSafely();

        target.pollAndWrite();

        verify(target, never()).writeToObserver(ArgumentMatchers.<ClientCallStreamObserver<V1.Span>>any(), ArgumentMatchers.<V1.Span>any());
    }

    @Test
    void pollAndWrite_ObserverReadySpanAvailableWrites() {
        SpanEvent spanEvent = buildSpanEvent();
        doReturn(true).when(target).awaitReadyObserver(observer);
        doReturn(spanEvent).when(target).pollSafely();

        target.pollAndWrite();

        verify(target).writeToObserver(observer, convert(spanEvent));
    }

    @Test
    void awaitReadyObserver_NotReadySleepsIncrementsCounter() {
        long startTime = System.currentTimeMillis();
        when(observer.isReady()).thenReturn(false);

        assertFalse(target.awaitReadyObserver(observer));
        assertTrue(System.currentTimeMillis() - startTime >= 250);
        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/NotReady");
    }

    @Test
    void awaitReadyObserver_IsReadyReturnsTrue() {
        when(observer.isReady()).thenReturn(true);

        assertTrue(target.awaitReadyObserver(observer));
    }

    @Test
    void pollSafely_InterruptedThrowsException() throws InterruptedException {
        doThrow(new InterruptedException()).when(queue).poll(anyLong(), ArgumentMatchers.<TimeUnit>any());

        assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() {
                target.pollSafely();
            }
        });
    }

    @Test
    void pollSafely_Valid() throws InterruptedException {
        SpanEvent spanEvent = buildSpanEvent();

        when(queue.poll(anyLong(), ArgumentMatchers.<TimeUnit>any())).thenReturn(spanEvent);

        assertEquals(spanEvent, target.pollSafely());
    }

    @Test
    void convert_Valid() throws IOException {
        SpanEvent spanEvent = buildSpanEvent();

        V1.Span result = convert(spanEvent);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.writeTo(baos);
        V1.Span deserialized = V1.Span.parseFrom(baos.toByteArray());

        assertEquals("abc123", deserialized.getTraceId());
        assertEquals("abc123", deserialized.getIntrinsicsOrThrow("traceId").getStringValue());
        assertEquals("my app", deserialized.getIntrinsicsOrThrow("appName").getStringValue());
        assertEquals("value", deserialized.getIntrinsicsOrThrow("intrStr").getStringValue());
        assertEquals(12345, deserialized.getIntrinsicsOrThrow("intrInt").getIntValue());
        assertEquals(3.14, deserialized.getIntrinsicsOrThrow("intrFloat").getDoubleValue(), 0.00001);
        assertTrue(deserialized.getIntrinsicsOrThrow("intrBool").getBoolValue());
        assertFalse(deserialized.containsIntrinsics("intrOther"));
    }

    private SpanEvent buildSpanEvent() {
        return SpanEvent.builder()
                .appName("my app")
                .putIntrinsic("traceId", "abc123")
                .putIntrinsic("intrStr", "value")
                .putIntrinsic("intrInt", 12345)
                .putIntrinsic("intrFloat", 3.14)
                .putIntrinsic("intrBool", true)
                .putIntrinsic("intrOther", TestEnum.ONE)
                .build();
    }

    private enum TestEnum { ONE }

    @Test
    void writeToObserver_RethrowsException() {
        doThrow(new RuntimeException("Error!")).when(observer).onNext(ArgumentMatchers.<V1.Span>any());

        assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() {
                target.writeToObserver(observer, V1.Span.newBuilder().build());
            }
        });
        verify(aggregator, never()).incrementCounter(anyString());
    }

    @Test
    void writeToObserver_NoExceptionIncrementsCounter() {
        target.writeToObserver(observer, V1.Span.newBuilder().build());

        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/Span/Sent");
    }

}