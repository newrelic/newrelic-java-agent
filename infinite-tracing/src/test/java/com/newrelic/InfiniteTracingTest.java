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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

}