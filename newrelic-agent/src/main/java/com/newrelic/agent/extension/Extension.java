/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.util.Collection;

import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.jmx.create.JmxConfiguration;

/**
 * Extensions are loaded from yml or xml files. Extensions must have a name, and they can optionally specify a version.
 * 
 * Extension files that are loaded from jar files will have a classloader that loads classes first through the parent
 * jar file.
 */
public abstract class Extension {
    private final String name;
    private final ClassLoader classloader;
    private final boolean custom;

    public Extension(ClassLoader classloader, String name, boolean custom) {
        if (name == null) {
            throw new IllegalArgumentException("Extensions must have a name");
        }
        this.classloader = classloader;
        this.name = name;
        this.custom = custom;
    }

    public final String getName() {
        return name;
    }

    /**
     * Returns this extension's classloader.
     * 
     */
    public final ClassLoader getClassLoader() {
        return classloader;
    }

    @Override
    public String toString() {
        return getName() + " Extension";
    }

    /**
     * Returns true if this is a user generated extension.
     * 
     */
    public boolean isCustom() {
        return custom;
    }

    public abstract boolean isEnabled();

    public abstract String getVersion();

    public abstract double getVersionNumber();

    public abstract Collection<JmxConfiguration> getJmxConfig();

    public abstract Collection<ExtensionClassAndMethodMatcher> getInstrumentationMatchers();
}
