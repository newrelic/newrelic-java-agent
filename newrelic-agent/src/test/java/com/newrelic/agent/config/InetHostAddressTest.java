package com.newrelic.agent.config;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InetHostAddressTest {

    @Test
    public void testNull() {
        assertNull(InetHostAddress.from(null));
    }

    @Test
    public void testGetAddressNull() {
        InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        when(interfaceAddress.getAddress()).thenReturn(null);

        assertNull(InetHostAddress.from(interfaceAddress));
    }

    @Test
    public void testGetInetHostAddress() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        when(interfaceAddress.getAddress()).thenReturn(localHost);

        assertEquals(InetHostAddress.from(interfaceAddress), localHost.getHostAddress());
    }
}