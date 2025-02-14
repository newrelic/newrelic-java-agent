/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.newrelic.agent.config.AgentConfigImpl.APPLICATION_LOGGING;

/* Default config should look like:
 *
 * application_logging:

 *   enabled: true
 *   forwarding:
 *     enabled: true
 *     max_samples_stored: 10000
 *     context_data:
 *       enabled: false
 *       include:
 *       exclude:
 *     labels:
 *       enabled: false
 *       exclude:
 *   metrics:
 *     enabled: true
 *   local_decorating:
 *     enabled: false
 */
public class ApplicationLoggingConfigImpl extends BaseConfig implements ApplicationLoggingConfig {
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + APPLICATION_LOGGING + ".";
    public static final String METRICS = "metrics";
    public static final String FORWARDING = "forwarding";
    public static final String LOCAL_DECORATING = "local_decorating";

    public static final boolean DEFAULT_ENABLED = true;
    public static final String ENABLED = "enabled";

    private final ApplicationLoggingMetricsConfig applicationLoggingMetricsConfig;
    private final ApplicationLoggingLocalDecoratingConfig applicationLoggingLocalDecoratingConfig;
    private final ApplicationLoggingForwardingConfig applicationLoggingForwardingConfig;

    private final boolean applicationLoggingEnabled;

    public ApplicationLoggingConfigImpl(Map<String, Object> pProps, boolean highSecurity) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        applicationLoggingEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        applicationLoggingMetricsConfig = createApplicationLoggingMetricsConfig();
        applicationLoggingLocalDecoratingConfig = createApplicationLoggingLocalDecoratingConfig();
        applicationLoggingForwardingConfig = createApplicationLoggingForwardingConfig(highSecurity);
    }

    private ApplicationLoggingMetricsConfig createApplicationLoggingMetricsConfig() {
        Map<String, Object> metricsProps = getProperty(METRICS, Collections.emptyMap());
        return new ApplicationLoggingMetricsConfig(metricsProps, SYSTEM_PROPERTY_ROOT);
    }

    private ApplicationLoggingLocalDecoratingConfig createApplicationLoggingLocalDecoratingConfig() {
        Map<String, Object> localDecoratingProps = getProperty(LOCAL_DECORATING, Collections.emptyMap());
        return new ApplicationLoggingLocalDecoratingConfig(localDecoratingProps, SYSTEM_PROPERTY_ROOT);
    }

    private ApplicationLoggingForwardingConfig createApplicationLoggingForwardingConfig(boolean highSecurity) {
        Map<String, Object> forwardingProps = getProperty(FORWARDING, Collections.emptyMap());
        return new ApplicationLoggingForwardingConfig(forwardingProps, SYSTEM_PROPERTY_ROOT, highSecurity);
    }

    static ApplicationLoggingConfigImpl createApplicationLoggingConfig(Map<String, Object> settings, boolean highSecurity) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new ApplicationLoggingConfigImpl(settings, highSecurity);
    }

    @Override
    public boolean isEnabled() {
        return applicationLoggingEnabled;
    }

    @Override
    public boolean isMetricsEnabled() {
        return applicationLoggingEnabled && applicationLoggingMetricsConfig.getEnabled();
    }

    @Override
    public boolean isLocalDecoratingEnabled() {
        return applicationLoggingEnabled && applicationLoggingLocalDecoratingConfig.getEnabled();
    }

    @Override
    public boolean isForwardingEnabled() {
        return applicationLoggingEnabled && applicationLoggingForwardingConfig.getEnabled();
    }

    @Override
    public int getMaxSamplesStored() {
        return applicationLoggingForwardingConfig.getMaxSamplesStored();
    }

    @Override
    public boolean isForwardingContextDataEnabled() {
        return applicationLoggingEnabled && applicationLoggingForwardingConfig.isContextDataEnabled();
    }

    @Override
    public List<String> getForwardingContextDataInclude() {
        return applicationLoggingForwardingConfig.contextDataInclude();
    }

    @Override
    public List<String> getForwardingContextDataExclude() {
        return applicationLoggingForwardingConfig.contextDataExclude();
    }

    @Override
    public boolean isLogLabelsEnabled() {
        return applicationLoggingEnabled && applicationLoggingForwardingConfig.isLogLabelsEnabled();
    }

    @Override
    public Map<String, String> removeExcludedLogLabels(Map<String, String> labels) {
        return applicationLoggingForwardingConfig.removeExcludedLabels(labels);
    }

    @Override
    public Set<String> getLogLabelsExcludeSet() {
        return applicationLoggingForwardingConfig.getLoggingLabelsExcludes();
    }
}

