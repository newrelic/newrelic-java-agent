/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;

/**
 * This class verifies that classloaders have visibility of our classes. Do not reference any Agent classes.
 */
public class ClassLoaderCheck {

    private static final String CLASSNAME = AgentBridge.class.getName();

    public static void loadAgentClass(ClassLoader loader) throws Throwable {
        if (loader != null) {
            loader.loadClass(ClassLoaderCheck.CLASSNAME);
        }
    }
}
