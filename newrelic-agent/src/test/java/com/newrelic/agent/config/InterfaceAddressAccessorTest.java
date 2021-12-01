package com.newrelic.agent.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class InterfaceAddressAccessorTest {

    @Test
    public void testNull() {
        assertNull(InterfaceAddressAccessor.getInetHostAddressFrom(null));
    }
}