/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.config.PointCutConfig;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.jmx.create.JmxConfiguration;
import com.newrelic.agent.jmx.create.JmxYmlParser;

/**
 * Extensions are loaded from yml files. Extensions must have a name, and they can optionally specify a version.
 * 
 * Extension yml files that are loaded from jar files will have a classloader that loads classes first through the
 * parent jar file.
 */
public class YamlExtension extends Extension {

    private final Config configuration;
    private final boolean enabled;

    public YamlExtension(ClassLoader classloader, String name, Map<String, Object> configuration, boolean custom)
            throws IllegalArgumentException {
        super(classloader, name, custom);
        if (name == null) {
            throw new IllegalArgumentException("Extensions must have a name");
        }
        this.configuration = new BaseConfig(configuration);
        this.enabled = this.configuration.getProperty("enabled", true);
    }

    YamlExtension(ClassLoader classloader, Map<String, Object> config, boolean custom) {
        this(classloader, (String) config.get("name"), config, custom);
    }

    @Override
    public String toString() {
        return getName() + " Extension";
    }

    public final Config getConfiguration() {
        return configuration;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getVersion() {
        return configuration.getProperty("version", "n/a");
    }

    @Override
    public double getVersionNumber() {
        try {
            return configuration.getProperty("version", 0.0);
        } catch (Exception e) {
            Agent.LOG.severe(MessageFormat.format("Extension \"{0}\" has an invalid version number: {1}: {2}",
                    getName(), e.getClass().getSimpleName(), e.getMessage()));
            return 0.0;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Collection<JmxConfiguration> getJmxConfig() {
        Object jmx = getConfiguration().getProperty("jmx");
        if ((jmx != null) && (jmx instanceof List)) {
            List<JmxConfiguration> list = new ArrayList<>();
            for (Map config : (List<Map>) jmx) {
                list.add(new JmxYmlParser(config));
            }
            return list;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<ExtensionClassAndMethodMatcher> getInstrumentationMatchers() {
        if (isEnabled()) {
            Object instrumentation = getConfiguration().getProperty("instrumentation");
            if (instrumentation instanceof Map) {
                return PointCutConfig.getExtensionPointCuts(this, (Map) instrumentation);
            } else if (configuration.getProperty("jmx", null) == null) {
                // Do not log error if extension has jmx config.
                String msg = MessageFormat.format(
                        "Extension {0} either does not have an instrumentation section or has an invalid instrumentation section. Please check the format of the file.",
                        getName());
                Agent.LOG.severe(msg);
            }
        }
        return Collections.emptyList();
    }
}
