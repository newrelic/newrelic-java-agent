package com.newrelic;

import com.newrelic.api.agent.Logger;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelSupplierTest {
    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }
    @Mock
    public ChannelFactory channelFactory;
    @Mock
    public ManagedChannel mockChannel;
    @Mock
    public ConnectionStatus connectionStatus;

    @Test
    public void shouldCallFactoryIfNotAlreadyExisting() throws InterruptedException {
        ChannelSupplier target = prepTargetForFirstCall();

        assertSame(mockChannel, target.get());
        verify(channelFactory, times(1)).createChannel();
    }

    @Test
    public void shouldNotCallFactoryIfAlreadyExisting() throws InterruptedException {
        ChannelSupplier target = prepTargetForFirstCall();
        assertSame(mockChannel, target.get());
        verify(channelFactory, times(1)).createChannel();

        when(connectionStatus.blockOnConnection()).thenReturn(ConnectionStatus.BlockResult.ALREADY_CONNECTED);
        assertSame(mockChannel, target.get());
        verify(channelFactory, times(1)).createChannel();
    }

    @Test
    public void shouldThrowIfGoingAway() throws InterruptedException {
        final ChannelSupplier target = prepTargetForFirstCall();
        reset(connectionStatus);
        when(connectionStatus.blockOnConnection()).thenReturn(ConnectionStatus.BlockResult.GO_AWAY_FOREVER);

        assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() {
                target.get();
            }
        });
    }

    @Test
    public void shutsDownOldChannel() throws InterruptedException {
        ChannelSupplier target = prepTargetForFirstCall();
        assertSame(mockChannel, target.get());

        when(channelFactory.createChannel()).thenReturn(mock(ManagedChannel.class));
        assertNotSame(mockChannel, target.get());
        verify(mockChannel, times(1)).shutdown();
    }

    public ChannelSupplier prepTargetForFirstCall() throws InterruptedException {
        when(channelFactory.createChannel()).thenReturn(mockChannel);
        when(connectionStatus.blockOnConnection()).thenReturn(ConnectionStatus.BlockResult.MUST_ATTEMPT_CONNECTION);
        return new ChannelSupplier(channelFactory, connectionStatus, mock(Logger.class));
    }

}