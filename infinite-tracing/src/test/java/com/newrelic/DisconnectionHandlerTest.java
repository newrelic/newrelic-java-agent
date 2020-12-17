package com.newrelic;

import com.newrelic.api.agent.Logger;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class DisconnectionHandlerTest {

    @Mock
    public ConnectionStatus connectionStatus;

    @Mock
    public BackoffPolicy defaultBackoffPolicy;

    @Mock
    public ConnectBackoffPolicy connectBackoffPolicy;

    public DisconnectionHandler target;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        target = new DisconnectionHandler(connectionStatus, defaultBackoffPolicy, connectBackoffPolicy, mock(Logger.class));;
    }

    @Test
    public void shouldNotBackOffIfCannotSetStatus() {
        when(defaultBackoffPolicy.shouldReconnect(null)).thenReturn(true);
        when(connectionStatus.shouldReconnect()).thenReturn(false);

        target.handle(null);

        verify(defaultBackoffPolicy, times(0)).backoff();
    }

    @Test
    public void shouldBackOff() {
        when(defaultBackoffPolicy.shouldReconnect(Status.ABORTED)).thenReturn(true);
        when(connectionStatus.shouldReconnect()).thenReturn(true);

        target.handle(Status.ABORTED);

        verify(defaultBackoffPolicy).backoff();
        verify(connectionStatus).reattemptConnection();
    }

    @Test
    public void shouldShutdownForeverOnUnimplemented() {
        when(defaultBackoffPolicy.shouldReconnect(Status.UNIMPLEMENTED)).thenReturn(false);

        target.handle(Status.UNIMPLEMENTED);

        verify(connectionStatus).shutDownForever();
    }

    @Test
    public void shouldConnectWithConnectBackoffPolicyForFailedPrecondition() {
        when(defaultBackoffPolicy.shouldReconnect(Status.UNIMPLEMENTED)).thenReturn(true);

        target.handle(Status.FAILED_PRECONDITION);

        verify(connectBackoffPolicy).backoff();
        verify(connectionStatus, times(1)).reattemptConnection();
    }
}