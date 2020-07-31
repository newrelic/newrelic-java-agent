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
 * This Weave class will match every ClassLoader that extends "java.lang.ClassLoader" and we specifically make sure
 * that it does not apply for "java.lang.ClassLoader" directly via our usage in {@link com.newrelic.agent.instrumentation.weaver.ClassLoaderClassTransformer}
 */
@Weave(type = MatchType.BaseClass, originalName = "java.lang.ClassLoader")
public abstract class AgentClassLoaderBaseInstrumentation {

    @WeaveAllConstructors
    protected AgentClassLoaderBaseInstrumentation() {

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
    public Class<?> loadClass(String name) {
        if (isNewRelicClass(name)) {
            Class<?> clazz = loadNewRelicClass(name);
            if (clazz != null) {
                return clazz;
            }
        }
        return Weaver.callOriginal();
    }

    /**
     * Intercept all calls to loadClass() and delegate any class loads for agent, weave or API classes to use the agent's
     * ClassLoader.
     *
     * @param name the name of the class we are loading
     * @return the loaded Class
     */
    protected Class<?> loadClass(String name, boolean resolve) {
        if (isNewRelicClass(name)) {
            Class<?> clazz = loadNewRelicClass(name);
            if (clazz != null) {
                return clazz;
            }
        }
        return Weaver.callOriginal();
    }

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
            // no-op
        }
        return null;
    }


}
