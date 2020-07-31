/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.weave.weavepackage.PackageValidationResult;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import com.newrelic.weave.weavepackage.WeavePackageManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegisterInstrumentationCloseableTest {
    private static AgentWeaverListener listener = new AgentWeaverListener(new NoOpWeaveViolationLogger());
    private static WeavePackageManager wpm = new WeavePackageManager(listener);
    private static WeavePackage testPackage;

    @BeforeClass
    public static void init() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        WeavePackageConfig config = WeavePackageConfig.builder().name("agent_unittest").source(
                "newrelic_agent.unit_test").build();
        testPackage = new WeavePackage(config, weaveBytes);
    }
    @Test
    public void testRegisterInstrumentationCloseable() {
        ToClose c1 = new ToClose();
        ToClose c2 = new ToClose();
        ToClose c3 = new ToClose();
        ToClose c4 = new ToClose();

        Assert.assertFalse(c1.wasClosed);
        listener.registerInstrumentationCloseable("na", null, c1);
        Assert.assertTrue(c1.wasClosed); // without a weavepackage to register to, this should be closed

        wpm.register(testPackage);
        listener.registerInstrumentationCloseable(testPackage.getName(), wpm.getWeavePackage(testPackage.getName()), c2);
        Assert.assertFalse(c2.wasClosed);
        listener.registerInstrumentationCloseable(testPackage.getName(), wpm.getWeavePackage(testPackage.getName()), c3);
        Assert.assertFalse(c3.wasClosed);

        Assert.assertFalse(c4.wasClosed);
        listener.registerInstrumentationCloseable("na", null, c4);
        Assert.assertTrue(c4.wasClosed); // without a weavepackage to register to, this should be closed

        wpm.deregister(testPackage);
        Assert.assertTrue(c2.wasClosed);
        Assert.assertTrue(c3.wasClosed);
    }

    private static class ToClose implements Closeable {
        public boolean wasClosed = false;

        @Override
        public void close() {
            wasClosed = true;
        }
    }

    private static class NoOpWeaveViolationLogger extends WeaveViolationLogger {
        public NoOpWeaveViolationLogger() {
            super(null);
        }

        @Override
        public void logWeaveViolations(PackageValidationResult packageResult, ClassLoader classloader, boolean isCustom) {
        }
    }
}
