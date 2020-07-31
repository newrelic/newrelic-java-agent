package com.newrelic;

import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChannelToStreamObserverTest {

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    public AtomicBoolean shouldRecreateCall = new AtomicBoolean();

    @Mock
    public StreamObserverFactory streamObserverFactory;
    @Mock
    public ManagedChannel mockChannel;
    @Mock
    public ClientCallStreamObserver<V1.Span> streamObserver;

    @SuppressWarnings("unchecked")
    public ClientCallStreamObserver<V1.Span> mockStreamObserver() {
        return (ClientCallStreamObserver<V1.Span>) mock(ClientCallStreamObserver.class);
    }

    @Test
    public void returnsNullImmediatelyWithNullChannel() {
        ChannelToStreamObserver target = new ChannelToStreamObserver(streamObserverFactory, shouldRecreateCall);
        shouldRecreateCall.set(false);
        when(streamObserverFactory.buildStreamObserver(null))
                .thenThrow(new AssertionError("~~ should not have been called ~~"));

        ClientCallStreamObserver<V1.Span> result = target.apply(null);
        assertNull(result);
        verifyNoInteractions(streamObserverFactory);
    }

    @Test
    public void createsAndReturnsNewStreamObserver() {
        ChannelToStreamObserver target = new ChannelToStreamObserver(streamObserverFactory, shouldRecreateCall);
        shouldRecreateCall.set(false);
        when(streamObserverFactory.buildStreamObserver(mockChannel)).thenReturn(streamObserver);

        ClientCallStreamObserver<V1.Span> result = target.apply(mockChannel);
        assertSame(streamObserver, result);
        verify(streamObserverFactory, times(1)).buildStreamObserver(mockChannel);
    }

    @Test
    public void doesNotRecreateStreamObserverIfCalledWithSameChannel() {
        ChannelToStreamObserver target = new ChannelToStreamObserver(streamObserverFactory, shouldRecreateCall);
        shouldRecreateCall.set(false);
        when(streamObserverFactory.buildStreamObserver(mockChannel)).thenReturn(streamObserver);

        ClientCallStreamObserver<V1.Span> result = target.apply(mockChannel);
        assertSame(streamObserver, result);
        verify(streamObserverFactory, times(1)).buildStreamObserver(mockChannel);

        result = target.apply(mockChannel);
        assertSame(streamObserver, result);
        verify(streamObserverFactory, times(1)).buildStreamObserver(mockChannel);
    }

    @Test
    public void recreatesStreamObserverIfRequired() {
        ChannelToStreamObserver target = new ChannelToStreamObserver(streamObserverFactory, shouldRecreateCall);
        ClientCallStreamObserver<V1.Span> mockObserver1 = mockStreamObserver();
        ClientCallStreamObserver<V1.Span> mockObserver2 = mockStreamObserver();

        shouldRecreateCall.set(false);
        when(streamObserverFactory.buildStreamObserver(any(ManagedChannel.class))).thenReturn(mockObserver1).thenReturn(mockObserver2);

        ClientCallStreamObserver<V1.Span> firstResult = target.apply(mockChannel);
        verify(streamObserverFactory, times(1)).buildStreamObserver(mockChannel);

        shouldRecreateCall.set(true);
        ClientCallStreamObserver<V1.Span> secondResult = target.apply(mockChannel);
        assertNotSame(secondResult, firstResult);
        verify(streamObserverFactory, times(2)).buildStreamObserver(mockChannel);
    }

    @Test
    public void recreatesStreamObserverIfCalledWithDifferentChannel() {
        ChannelToStreamObserver target = new ChannelToStreamObserver(streamObserverFactory, shouldRecreateCall);
        ClientCallStreamObserver<V1.Span> mockObserver1 = mockStreamObserver();
        ClientCallStreamObserver<V1.Span> mockObserver2 = mockStreamObserver();

        shouldRecreateCall.set(false);
        when(streamObserver.isReady()).thenReturn(true);
        when(streamObserverFactory.buildStreamObserver(any(ManagedChannel.class))).thenReturn(mockObserver1).thenReturn(mockObserver2);

        ClientCallStreamObserver<V1.Span> result = target.apply(mockChannel);
        verify(streamObserverFactory, times(1)).buildStreamObserver(mockChannel);

        ManagedChannel newChannel = mock(ManagedChannel.class);
        ClientCallStreamObserver<V1.Span> newResult = target.apply(newChannel);
        assertNotSame(result, newResult);
        verify(streamObserverFactory, times(1)).buildStreamObserver(newChannel);
    }

}