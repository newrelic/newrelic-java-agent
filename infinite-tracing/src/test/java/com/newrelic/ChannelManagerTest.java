package com.newrelic;

import com.google.common.collect.ImmutableMap;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.IngestServiceGrpc.IngestServiceStub;
import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
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
    private ClientCallStreamObserver<V1.Span> spanObserver;
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
        doReturn(spanObserver).when(stub).recordSpan(responseObserver);
    }

    @Test
    void getSpanObserver_ShutdownThrowsException() {
        target.shutdownChannelForever();

        assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() {
                target.getSpanObserver();
            }
        });
    }

    @Test
    void getSpanObserver_BuildsChannelAndSpanObserverWhenMissing() {
        assertEquals(spanObserver, target.getSpanObserver());
        assertEquals(spanObserver, target.getSpanObserver());

        verify(target).buildChannel();
        verify(target).buildStub(managedChannel);
        verify(target).buildResponseObserver();
        verify(stub, times(1)).recordSpan(responseObserver);
        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/Connect");
    }

    @Test
    void getSpanObserver_RecreatesSpanObserver() {
        assertEquals(spanObserver, target.getSpanObserver());
        target.recreateSpanObserver();
        assertEquals(spanObserver, target.getSpanObserver());
        assertEquals(spanObserver, target.getSpanObserver());

        verify(target).buildChannel();
        verify(target, times(2)).buildStub(managedChannel);
        verify(spanObserver).cancel(eq("CLOSING_CONNECTION"), any(ChannelClosingException.class));
        verify(target, times(2)).buildResponseObserver();
        verify(stub, times(2)).recordSpan(responseObserver);
        verify(aggregator, times(2)).incrementCounter("Supportability/InfiniteTracing/Connect");
    }

    @Test
    @Timeout(15)
    void getSpanObserver_AwaitsBackoff() throws ExecutionException, InterruptedException {
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
        Future<ClientCallStreamObserver<V1.Span>> futureSpanObserver = executorService.submit(new Callable<ClientCallStreamObserver<V1.Span>>() {
            @Override
            public ClientCallStreamObserver<V1.Span> call() {
                try {
                    // Wait for the backoff task to have initiated backoff
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread interrupted while sleeping.");
                }

                ClientCallStreamObserver<V1.Span> response = target.getSpanObserver();
                getSpanObserverCompletedAt.set(System.currentTimeMillis());
                return response;
            }
        });

        backoffFuture.get();
        assertEquals(spanObserver, futureSpanObserver.get());
        assertTrue(backoffCompletedAt.get() > 0);
        assertTrue(getSpanObserverCompletedAt.get() > 0);
        assertTrue(getSpanObserverCompletedAt.get() >= backoffCompletedAt.get());
    }

    @Test
    void shutdownChannelAndBackoff_ShutsDownChannelCancelsSpanObserver() {
        assertEquals(spanObserver, target.getSpanObserver());
        target.shutdownChannelAndBackoff(0);
        assertEquals(spanObserver, target.getSpanObserver());

        verify(target).recreateSpanObserver();
        // Channel and span observer is built twice
        verify(target, times(2)).buildChannel();
        verify(target, times(2)).buildStub(managedChannel);
        verify(target, times(2)).buildResponseObserver();
        verify(stub, times(2)).recordSpan(responseObserver);
        verify(aggregator, times(2)).incrementCounter("Supportability/InfiniteTracing/Connect");
    }

}