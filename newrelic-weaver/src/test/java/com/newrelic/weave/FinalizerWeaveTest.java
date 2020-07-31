/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * FinalizerWeaveTest.java
 */
public class FinalizerWeaveTest {

    public static boolean wasFinalized;

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassWeave implementationWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.FinalizerWeaveTest$Original", "com.newrelic.weave.FinalizerWeaveTest$Weave");
        assertNull(implementationWeave.getMatch().getExtension());
    }

    @Test
    public void testFinalizers() throws InterruptedException {
        new Original();
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(10);
        }
        assertTrue(wasFinalized);
    }

    public static class Original {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }
    }

    public static class Weave {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            wasFinalized = true;
        }
    }
}
