package com.newrelic.agent.service.module;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JarCollectorConnectionListenerTest {
    @Test
    public void resetsOnDefaultAppName() {
        AtomicBoolean shouldReset = new AtomicBoolean(false);
        JarCollectorConnectionListener target = new JarCollectorConnectionListener("default", shouldReset);
        target.onEstablished("default", null, null);
        assertTrue(shouldReset.get());
    }

    @Test
    public void doesNotResetOnOtherAppName() {
        AtomicBoolean shouldReset = new AtomicBoolean(false);
        JarCollectorConnectionListener target = new JarCollectorConnectionListener("default", shouldReset);
        target.onEstablished("other", null, null);
        assertFalse(shouldReset.get());
    }

}