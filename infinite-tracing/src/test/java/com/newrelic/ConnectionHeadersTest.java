package com.newrelic;

import com.newrelic.api.agent.Logger;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConnectionHeadersTest {
    @Test
    public void shouldUpdateConnectionStatus() {
        ConnectionStatus connectionStatus = mock(ConnectionStatus.class);
        ConnectionHeaders target = new ConnectionHeaders(connectionStatus, mock(Logger.class), "license key abc");

        assertNull(target.get());

        target.set("bar", Collections.singletonMap("OTHER_KEY", "other value"));
        verify(connectionStatus).reattemptConnection();
        assertEquals("other value", target.get().get("OTHER_KEY"));
        assertEquals("bar", target.get().get("agent_run_token"));
        assertEquals("license key abc", target.get().get("license_key"));
    }
}