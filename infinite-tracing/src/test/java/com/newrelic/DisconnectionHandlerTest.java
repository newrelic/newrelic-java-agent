package com.newrelic;

import com.newrelic.api.agent.Logger;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisconnectionHandlerTest {
    @Test
    public void shouldNotBackOffIfCannotSetStatus() {
        DisconnectionHandler target = new DisconnectionHandler(connectionStatus, backoffPolicy, mock(Logger.class));
        when(connectionStatus.shouldReconnect()).thenReturn(false);
        target.handle(null);
    }

    @Test
    public void shutdownIfShouldShutdown() {
        DisconnectionHandler target = new DisconnectionHandler(connectionStatus, backoffPolicy, mock(Logger.class));
        when(connectionStatus.shouldReconnect()).thenReturn(true);
        when(backoffPolicy.shouldReconnect(Status.ABORTED)).thenReturn(false);
        target.handle(Status.ABORTED);
        verify(connectionStatus).shutDownForever();
    }

    @Test
    public void shouldBackOff() {
        DisconnectionHandler target = new DisconnectionHandler(connectionStatus, backoffPolicy, mock(Logger.class));
        when(connectionStatus.shouldReconnect()).thenReturn(true);
        when(backoffPolicy.shouldReconnect(Status.ABORTED)).thenReturn(true);
        target.handle(Status.ABORTED);
        verify(backoffPolicy).shouldReconnect(Status.ABORTED);
        verify(connectionStatus).reattemptConnection();
    }

    @Test
    public void shouldShutdownForeverOnUnimplemented() {
        DisconnectionHandler target = new DisconnectionHandler(connectionStatus, backoffPolicy, mock(Logger.class));
        when(connectionStatus.shouldReconnect()).thenReturn(true);
        when(backoffPolicy.shouldReconnect(Status.UNIMPLEMENTED)).thenReturn(false);
        target.handle(Status.UNIMPLEMENTED);
        verify(connectionStatus).shutDownForever();
    }

    @Mock
    public ConnectionStatus connectionStatus;

    @Mock
    public BackoffPolicy backoffPolicy;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }
}