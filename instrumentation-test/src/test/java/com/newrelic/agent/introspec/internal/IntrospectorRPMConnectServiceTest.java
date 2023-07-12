package com.newrelic.agent.introspec.internal;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class IntrospectorRPMConnectServiceTest {

    private IntrospectorRPMConnectService introspectorRPMConnectService;

    @Before
    public void setUp() throws Exception {
        introspectorRPMConnectService = new IntrospectorRPMConnectService();
    }

    @Test
    public void testIsEnabled() {
        // Check if isEnabled method always returns true
        assertTrue(introspectorRPMConnectService.isEnabled());
    }

    @Test
    public void testConnect() {
        // Since connect method is empty, we're just making sure calling it doesn't produce an exception
        introspectorRPMConnectService.connect(null);
    }

    @Test
    public void testConnectImmediate() {
        // Since connectImmediate method is empty, we're just making sure calling it doesn't produce an exception
        introspectorRPMConnectService.connectImmediate(null);
    }

    @Test
    public void testDoStart() throws Exception {
        // Since doStart method is empty, we're just making sure calling it doesn't produce an exception
        introspectorRPMConnectService.doStart();
    }

    @Test
    public void testDoStop() throws Exception {
        // Since doStop method is empty, we're just making sure calling it doesn't produce an exception
        introspectorRPMConnectService.doStop();
    }
}

