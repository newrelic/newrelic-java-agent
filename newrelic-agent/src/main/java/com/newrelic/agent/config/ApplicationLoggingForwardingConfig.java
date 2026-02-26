/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ApplicationLoggingForwardingConfig extends BaseConfig {
    public static final String ROOT = "forwarding";
    public static final String ENABLED = "enabled";
    public static final String MAX_SAMPLES_STORED = "max_samples_stored";
    public static final String LOG_LEVEL_DENYLIST = "log_level_denylist";

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_MAX_SAMPLES_STORED = 10000;
    public static final String DEFAULT_LOG_LEVEL_DENYLIST = "";

    private final boolean enabled;
    private final int maxSamplesStored;
    private final ApplicationLoggingContextDataConfig contextDataConfig;
    private final ApplicationLoggingLabelsConfig loggingLabelsConfig;
    private final Set<String> logLevelDenylist;

    public ApplicationLoggingForwardingConfig(Map<String, Object> props, String parentRoot, boolean highSecurity) {
        super(props, parentRoot + ROOT + ".");
        maxSamplesStored = initMaxSamplesStored();
        boolean storedMoreThan0 = maxSamplesStored > 0;
        enabled = storedMoreThan0 && !highSecurity && getProperty(ENABLED, DEFAULT_ENABLED);
        contextDataConfig = createContextDataConfig(highSecurity);
        loggingLabelsConfig = createLoggingLabelsConfig();
        logLevelDenylist = createLogLevelDenylist();
    }

    private int initMaxSamplesStored() {
        try {
            return getProperty(MAX_SAMPLES_STORED, DEFAULT_MAX_SAMPLES_STORED);
        } catch (ClassCastException classCastException) {
            Agent.LOG.log(Level.WARNING, "The max_samples_stored was likely too large {0}, we will use default {1}",
                    getProperty(MAX_SAMPLES_STORED), DEFAULT_MAX_SAMPLES_STORED);
            return DEFAULT_MAX_SAMPLES_STORED;
        }
    }

    private ApplicationLoggingContextDataConfig createContextDataConfig(boolean highSecurity) {
        Map<String, Object> contextDataProps = getProperty(ApplicationLoggingContextDataConfig.ROOT, Collections.emptyMap());
        return new ApplicationLoggingContextDataConfig(contextDataProps, systemPropertyPrefix, highSecurity);
    }

    private ApplicationLoggingLabelsConfig createLoggingLabelsConfig() {
        Map<String, Object> labelsProps = getProperty(ApplicationLoggingLabelsConfig.ROOT, Collections.emptyMap());
        return new ApplicationLoggingLabelsConfig(labelsProps, systemPropertyPrefix);
    }

    private Set<String> createLogLevelDenylist() {
        Set<String> denyList = new HashSet<>();
        String[] levels = getProperty(LOG_LEVEL_DENYLIST, DEFAULT_LOG_LEVEL_DENYLIST).split(",");
        for (String level : levels) {
            String levelUppercase = level.trim().toUpperCase();
            if (!levelUppercase.isEmpty()) {
                denyList.add(levelUppercase);
            }
        }
        return Collections.unmodifiableSet(denyList);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    public boolean isContextDataEnabled() {
        return enabled && contextDataConfig.getEnabled();
    }

    public List<String> contextDataInclude() {
        return contextDataConfig.getInclude();
    }

    public List<String> contextDataExclude() {
        return contextDataConfig.getExclude();
    }

    public boolean isLogLabelsEnabled() {
        return enabled && loggingLabelsConfig.getEnabled();
    }

    public Map<String, String> removeExcludedLabels(Map<String, String> labels) {
        return loggingLabelsConfig.removeExcludedLabels(labels);
    }

    public Set<String> getLoggingLabelsExcludeSet() {
        return loggingLabelsConfig.getExcludeSet();
    }

    /**
     * @return An unmodifiable set of log levels that should be blocked from being sent to New Relic.
     */
    public Set<String> getLogLevelDenylist() {
        return logLevelDenylist;
    }
}