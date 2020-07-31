/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.newrelic.weave.WeaveTestUtils;
import com.sun.tools.attach.VirtualMachine;

/**
 * A simple java agent which uses agentmain to load at runtime.
 */
public class WeaverAgent {
    private static Instrumentation instrumentation = null;

    /**
     * Retrieve the reference to java.lang.instrument.Instrumentation.
     * When called for the first time, this will cause the JVM to
     * invoke agentmain to supply the Instrumentation.
     */
    public static Instrumentation getInstrumentation() {
        if (null == instrumentation) {
            synchronized (WeaverAgent.class) {
                if (null == instrumentation) {
                    try {
                        loadAgent();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
        return instrumentation;
    }

    private static void loadAgent() throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Agent-Class", WeaverAgent.class.getName());
        manifest.getMainAttributes().putValue("Can-Retransform-Classes", "true");
        manifest.getMainAttributes().putValue("Can-Redefine-Classes", "true");
        URL agentJar = WeaveTestUtils.createJarFile("weaveragent", manifest, WeaverAgent.class);

        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        String pid = nameOfRunningVM.substring(0, p);
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(agentJar.toExternalForm().replaceFirst("^file:", ""), "");
            vm.detach();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoked by the JVM at runtime. Stores a reference to java.lang.instrument.Instrumentation.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

}
