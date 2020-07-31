package com.newrelic;

import com.newrelic.agent.interfaces.backport.Supplier;
import com.newrelic.trace.v1.V1;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreamObserverSupplierTest {
    @Mock
    public Supplier<ManagedChannel> channelSupplier;
    @Mock
    public Function<ManagedChannel, ClientCallStreamObserver<V1.Span>> channelToStreamObserverConverter;
    @Mock
    public ManagedChannel managedChannel;
    @Mock
    public ClientCallStreamObserver<V1.Span> streamObserver;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void relaysCallsThrough() {
        when(channelSupplier.get()).thenReturn(managedChannel);
        when(channelToStreamObserverConverter.apply(managedChannel)).thenReturn(streamObserver);

        StreamObserverSupplier target = new StreamObserverSupplier(channelSupplier, channelToStreamObserverConverter);
        assertSame(streamObserver, target.get());
        verify(channelSupplier).get();
        verify(channelToStreamObserverConverter).apply(managedChannel);
    }
}