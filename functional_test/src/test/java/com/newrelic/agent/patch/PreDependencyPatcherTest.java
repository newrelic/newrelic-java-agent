/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.patch;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.BeforeClass;
import org.junit.Test;

public class PreDependencyPatcherTest {

    private static PrintStream printStream;
    private static ByteArrayOutputStream outputStream;

    @BeforeClass
    public static void setup() {
        System.setProperty("java.util.logging.manager", "InvalidClass!");

        outputStream = new ByteArrayOutputStream();
        printStream = new PrintStream(outputStream);
        System.setErr(printStream);
    }

    @Test
    public void testInitalized() throws Exception {
        Class.forName("com.google.common.reflect.ClassPath");
        printStream.flush();
        // Assert.assertTrue(outputStream.size() > 0);
    }
}
