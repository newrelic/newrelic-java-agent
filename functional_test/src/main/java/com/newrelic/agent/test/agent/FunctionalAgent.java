/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.test.agent;

import java.lang.instrument.Instrumentation;

/**
 * An agent that runs in the functional tests before the java agent. This allows us to alter the state of the jvm before
 * the agent runs.
 */
public class FunctionalAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        preAgentSetup(agentArgs);
        System.out.println("FunctionalAgent initialized");
    }

    public static void preAgentSetup(String args) {
        try {
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("com.newrelic.api.jruby.DoNothingClassThatExistsForTesting");
        } catch (Exception e) {
            System.out.println("FunctionalAgent error: " + e);
            e.printStackTrace();
        }
    }

}
