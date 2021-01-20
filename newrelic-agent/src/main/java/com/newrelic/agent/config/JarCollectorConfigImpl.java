/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class JarCollectorConfigImpl extends BaseConfig implements JarCollectorConfig {

    public static final String ENABLED = "enabled";
    public static final String SKIP_TEMP_JARS = "skip_temp_jars";
    public static final String JARS_PER_SECOND = "jars_per_second";

    public static final boolean DEFAULT_ENABLED = Boolean.TRUE;
    public static final boolean DEFAULT_SKIP_TEMP_JARS = Boolean.TRUE;
    public static final int DEFAULT_JARS_PER_SECOND = 10;

    // The newrelic.config.module root shouldn't be used but is kept for backwards compatibility
    public static final String SYSTEM_PROPERTY_ROOT_DEPRECATED = "newrelic.config.module."; // NEW_RELIC_MODULE_
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.jar_collector."; // NEW_RELIC_JAR_COLLECTOR_
    private static final AtomicBoolean isUsingDeprecatedConfigSettings = new AtomicBoolean(false);

    private final boolean isEnabled;
    private final boolean skipTempJars;
    private final Integer jarsPerSecond;

    public JarCollectorConfigImpl(Map<String, Object> pProps) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        skipTempJars = getProperty(SKIP_TEMP_JARS, DEFAULT_SKIP_TEMP_JARS);
        jarsPerSecond = getProperty(JARS_PER_SECOND, DEFAULT_JARS_PER_SECOND);
    }

    // This method gets hit multiple times due to merging local and server side configs
    static JarCollectorConfigImpl createJarCollectorConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new JarCollectorConfigImpl(settings);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean skipTempJars() {
        return skipTempJars;
    }

    @Override
    public int getJarsPerSecond() {
        return jarsPerSecond;
    }

    @Override
    protected Object getPropertyFromSystemEnvironment(String name, Object defaultVal) {
        return getMergedValue(name, true);
    }

    @Override
    protected Object getPropertyFromSystemProperties(String name, Object defaultVal) {
        return getMergedValue(name, false);
    }

    /**
     * Merges the values for a given config setting if it was set in multiple places. Precedence is given to values configured using the
     * 'NEW_RELIC_JAR_COLLECTOR_' environment variable and 'newrelic.config.jar_collector.' system property prefixes over the deprecated
     * 'NEW_RELIC_MODULE_' and 'newrelic.config.module.' options.
     *
     * @param name Key representing a jar collector config property
     * @param isEnvVar boolean indicating if the property is being parsed from an environment variable or system property
     * @return Object representing parsed config value
     */
    private Object getMergedValue(String name, boolean isEnvVar) {
        if (systemPropertyPrefix == null) {
            return null;
        }

        final SystemPropertyProvider systemPropertyProvider = SystemPropertyFactory.getSystemPropertyProvider();

        final String systemPropertyKey = getSystemPropertyKey(name);
        final String deprecatedSystemPropertyKey = getDeprecatedSystemPropertyKey(name);

        final Object parsedValue;
        final Object deprecatedParsedValue;

        if (isEnvVar) {
            parsedValue = parseValue(systemPropertyProvider.getEnvironmentVariable(systemPropertyKey));
            deprecatedParsedValue = parseValue(systemPropertyProvider.getEnvironmentVariable(deprecatedSystemPropertyKey));
        } else {
            parsedValue = parseValue(systemPropertyProvider.getSystemProperty(systemPropertyKey));
            deprecatedParsedValue = parseValue(systemPropertyProvider.getSystemProperty(deprecatedSystemPropertyKey));
        }

        if (parsedValue != null) {
            return parsedValue;
        } else {
            if (deprecatedParsedValue != null) {
                // We can't actually log here due to hitting this code path several times as we'll see duplicated log entries,
                // instead record that a deprecated setting was found and log a warning in JarCollectorServiceImpl
                isUsingDeprecatedConfigSettings.set(true);
            }
            return deprecatedParsedValue;
        }
    }

    /**
     * Prefix a given key with the deprecated config newrelic.config.module.
     *
     * @param key The jar collector config property
     * @return String A complete deprecated system property
     */
    private String getDeprecatedSystemPropertyKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        return SYSTEM_PROPERTY_ROOT_DEPRECATED + key;
    }

    /**
     * Used to determine if a deprecated config setting was used
     *
     * @return true if a deprecated config setting was found, otherwise false
     */
    public static boolean isUsingDeprecatedConfigSettings() {
        return isUsingDeprecatedConfigSettings.get();
    }
}
