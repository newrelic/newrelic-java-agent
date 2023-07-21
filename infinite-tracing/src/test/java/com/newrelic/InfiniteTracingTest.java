package com.newrelic;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfiniteTracingTest {

    @Mock
    private Logger logger;
    @Mock
    private InfiniteTracingConfig config;
    @Mock
    private MetricAggregator aggregator;
    @Mock
    private ExecutorService executorService;
    @Mock
    private ChannelManager channelManager;
    @Mock
    private SpanEventSender spanEventSender;

    private LinkedBlockingDeque<SpanEvent> queue;
    private InfiniteTracing target;
    private static final String serviceName = "TestService";

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        when(config.getLogger()).thenReturn(logger);
        queue = new LinkedBlockingDeque<>(1);
        target = spy(new InfiniteTracing(config, aggregator, executorService, queue));
        doReturn(channelManager).when(target).buildChannelManager(anyString(), ArgumentMatchers.<String, String>anyMap());
        doReturn(spanEventSender).when(target).buildSpanEventSender();

    }

    @Test
    void startAndStop() {
        Future future = mock(Future.class);
        when(executorService.submit(ArgumentMatchers.<Runnable>any())).thenReturn(future);

        target.start("token1", ImmutableMap.of("key1", "value1"));
        target.start("token2", ImmutableMap.of("key2", "value2"));

        verify(target).buildChannelManager("token1", ImmutableMap.of("key1", "value1"));
        verify(target).buildSpanEventSender();
        verify(executorService).submit(spanEventSender);
        verify(channelManager).updateMetadata("token2", ImmutableMap.of("key2", "value2"));
        verify(channelManager).shutdownChannelAndBackoff(0);

        target.stop();
        target.stop();

        verify(future).cancel(true);
        verify(channelManager).shutdownChannelForever();
    }

    @Test
    @Timeout(1)
    void accept_IncrementsCounterAndOffersToQueue() {
        SpanEvent spanEvent = SpanEvent.builder().build();

        target.accept(spanEvent);

        verify(aggregator).incrementCounter("Supportability/InfiniteTracing/Span/Seen");
        assertEquals(spanEvent, queue.poll());
    }

    @Test
    public void buildChannelManager_returnsChannelManager() {
        target = new InfiniteTracing(config, aggregator, executorService, queue);
        assertNotNull(target.buildChannelManager("1234", ImmutableMap.of("key1", "value1")));
    }

    @Test
    public void buildSpanEventSender_returnsSpanEventSender() {
        target = new InfiniteTracing(config, aggregator, executorService, queue);
        assertNotNull(target.buildSpanEventSender());
    }

    @Test
    public void testNewThread() {
        InfiniteTracing.DaemonThreadFactory factory = new InfiniteTracing.DaemonThreadFactory(serviceName);
        Runnable runnable = () -> {
        };

        Thread thread = factory.newThread(runnable);
        assertNotNull(thread);
        assertTrue(thread.isDaemon());
        assertEquals("New Relic " + serviceName + " #" + 1, thread.getName());

    }

}