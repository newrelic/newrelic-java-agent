package com.newrelic;

import com.google.common.collect.ImmutableMap;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.IngestServiceGrpc.IngestServiceStub;
import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelManagerTest {

    @Mock
    private Logger logger;
    @Mock
    private InfiniteTracingConfig config;
    @Mock
    private MetricAggregator aggregator;
    @Mock
    private ManagedChannel managedChannel;
    @Mock
    private ClientCallStreamObserver<V1.Span> spanStreamObserver;
    @Mock
    private ClientCallStreamObserver<V1.SpanBatch> spanBatchStreamObserver;
    @Mock
    private Observer spanObserver;
    @Mock
    private SpanBatchObserver spanBatchObserver;
    @Mock
    private ResponseObserver responseObserver;
    @Mock
    private IngestServiceStub stub;

    private ChannelManager target;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        when(config.getLogger()).thenReturn(logger);
        target = spy(new ChannelManager(config, aggregator, "agentToken", ImmutableMap.of("key1", "value1")));
        doReturn(managedChannel).when(target).buildChannel();
        doReturn(stub).when(target).buildStub(managedChannel);
        doReturn(responseObserver).when(target).buildResponseObserver();
        doReturn(spanStreamObserver).when(stub).recordSpan(responseObserver);
        doReturn(spanBatchStreamObserver).when(stub).recordSpanBatch(responseObserver);
        doReturn(spanObserver).when(target).buildSpanObserver(spanStreamObserver);
        doReturn(spanBatchObserver).when(target).buildSpanBatchObserver(spanBatchStreamObserver);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void getSpanObserver_ShutdownThrowsException(boolean batchingEnabled) {
        setupBatchingAndGetExpectedObserver(batchingEnabled);

        target.shutdownChannelForever();

        assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() {
                target.getObserver();
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void getSpanObserver_BuildsChannelAndSpanObserverWhenMissing(boolean batchingEnabled) {
        final Observer expectedObserver = setupBatchingAndGetExpectedObserver(batchingEnabled);
        assertEquals(expectedObserver, target.getObserver());
        assertEquals(expectedObserver, target.getObserver());

        verify(target).buildChannel();
        verify(target).buildStub(managedChannel);
        verify(target).buildResponseObserver();
        verifyExpectedRecordMethod(1, batchingEnabled);
        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/Connect");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void getSpanObserver_RecreatesSpanObserver(boolean batchingEnabled) {
        final Observer expectedObserver = setupBatchingAndGetExpectedObserver(batchingEnabled);
        assertEquals(expectedObserver, target.getObserver());
        target.recreateSpanObserver();
        assertEquals(expectedObserver, target.getObserver());
        assertEquals(expectedObserver, target.getObserver());

        verify(target).buildChannel();
        verify(target, times(2)).buildStub(managedChannel);
        verify(expectedObserver).cancel(eq("CLOSING_CONNECTION"), any(ChannelClosingException.class));
        verify(target, times(2)).buildResponseObserver();
        verifyExpectedRecordMethod(2, batchingEnabled);
        verify(aggregator, times(2)).incrementCounter("Supportability/InfiniteTracing/Connect");
    }

    @ParameterizedTest
    @Timeout(15)
    @ValueSource(booleans = { false, true })
    void getSpanObserver_AwaitsBackoff(boolean batchingEnabled) throws ExecutionException, InterruptedException {
        final Observer expectedObserver = setupBatchingAndGetExpectedObserver(batchingEnabled);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // Submit a task to initiate a backoff
        final AtomicLong backoffCompletedAt = new AtomicLong();
        Future<?> backoffFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                target.shutdownChannelAndBackoff(5);
                backoffCompletedAt.set(System.currentTimeMillis());
            }
        });

        // Obtain a span observer in another thread, confirming it waits for the backoff to complete
        final AtomicLong getSpanObserverCompletedAt = new AtomicLong();
        Future<Observer> futureSpanObserver = executorService.submit(new Callable<Observer>() {
            @Override
            public Observer call() {
                try {
                    // Wait for the backoff task to have initiated backoff
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread interrupted while sleeping.");
                }

                Observer response = target.getObserver();
                getSpanObserverCompletedAt.set(System.currentTimeMillis());
                return response;
            }
        });

        backoffFuture.get();
        assertEquals(expectedObserver, futureSpanObserver.get());
        assertTrue(backoffCompletedAt.get() > 0);
        assertTrue(getSpanObserverCompletedAt.get() > 0);
        assertTrue(getSpanObserverCompletedAt.get() >= backoffCompletedAt.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void shutdownChannelAndBackoff_ShutsDownChannelCancelsSpanObserver(boolean batchingEnabled) {
        final Observer expectedObserver = setupBatchingAndGetExpectedObserver(batchingEnabled);

        assertEquals(expectedObserver, target.getObserver());
        target.shutdownChannelAndBackoff(0);
        assertEquals(expectedObserver, target.getObserver());

        verify(target).recreateSpanObserver();
        // Channel and span observer is built twice
        verify(target, times(2)).buildChannel();
        verify(target, times(2)).buildStub(managedChannel);
        verify(target, times(2)).buildResponseObserver();
        verifyExpectedRecordMethod(2, batchingEnabled);
        verify(aggregator, times(2)).incrementCounter("Supportability/InfiniteTracing/Connect");
    }

    private Observer setupBatchingAndGetExpectedObserver(boolean batchingEnabled) {
        when(config.getUseBatching()).thenReturn(batchingEnabled);
        return batchingEnabled ? spanBatchObserver : spanObserver;
    }

    private void verifyExpectedRecordMethod(int times, boolean batchingEnabled) {
        if (batchingEnabled) {
            verify(stub, times(times)).recordSpanBatch(responseObserver);
        } else {
            verify(stub, times(times)).recordSpan(responseObserver);
        }
    }

}
