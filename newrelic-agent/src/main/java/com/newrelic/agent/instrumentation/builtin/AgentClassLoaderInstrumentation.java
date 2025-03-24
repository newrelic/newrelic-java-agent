/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.builtin;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * This Weave class exists separately from {@link AgentClassLoaderBaseInstrumentation} because we only want to instrument
 * one of the loadClass() methods for "java.lang.ClassLoader". This comes from the previous ClassLoaderClassTransformer
 * code and most likely exists to reduce the possibility of StackOverflowErrors with loadClass calls.
 */
@Weave(type = MatchType.ExactClass, originalName = "java.lang.ClassLoader")
public abstract class AgentClassLoaderInstrumentation {

    @WeaveAllConstructors
    protected AgentClassLoaderInstrumentation() {

    }

    /**
     * If the class we are trying to load starts with "com.newrelic.api.agent", "com.newrelic.agent" or "com.newrelic.weave" return true.
     *
     * @param className the class name of the class we are trying to load
     * @return true if the class matches one of our prefixes
     */
    private boolean isNewRelicClass(String className) {
        return className != null && (className.startsWith("com.newrelic.api.agent.") ||
                className.startsWith("com.newrelic.agent.") || className.startsWith("com.newrelic.weave."));
    }

    /**
     * Intercept all calls to loadClass() and delegate any class loads for agent, weave or API classes to use the agent's
     * ClassLoader.
     *
     * @param name the name of the class we are loading
     * @return the loaded Class
     */
//    public Class<?> loadClass(String name) throws ClassNotFoundException {
////        if (isNewRelicClass(name)) {
////            Class<?> clazz = loadNewRelicClass(name);
////            if (clazz != null) {
////                return clazz;
////            }
////        }
//        return Weaver.callOriginal();
//    }

    /**
     * Delegate class loading to the agent's ClassLoader if we aren't currently trying to hijack this ClassLoader.
     *
     * @param className the class we are loading
     * @return the Class loaded on the Agent's ClassLoader
     */
    private Class<?> loadNewRelicClass(String className) {
        try {
            ClassLoader classLoader = AgentBridge.getAgent().getClass().getClassLoader();
            Object agentClassLoader = classLoader;
            if (this != agentClassLoader) {
                return classLoader.loadClass(className);
            }
        } catch (Throwable t) {
        }
        return null;
    }

}
