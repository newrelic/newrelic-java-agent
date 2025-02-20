/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApplicationLoggingLabelsConfig extends BaseConfig {
    public static final String ROOT = "labels";
    public static final String ENABLED = "enabled";
    public static final String EXCLUDE = "exclude";
    public static final boolean DEFAULT_ENABLED = false;

    private final boolean enabled;
    private final Set<String> excludeSet;

    /**
     * Constructor to initialize LogLabelsConfig from configuration properties.
     *
     * @param props      Map containing configuration properties.
     * @param parentRoot Root path for the configuration properties.
     */
    public ApplicationLoggingLabelsConfig(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot + ROOT + ".");

        excludeSet = initExcludes(getProperty(EXCLUDE));
        enabled = getProperty(ENABLED, DEFAULT_ENABLED);
    }

    private Set<String> initExcludes(Object excludes) {
        Set<String> formattedExcludes = new HashSet<>();
        if (excludes instanceof List<?>) {
            for (Object listItem : (List<?>) excludes) {
                formattedExcludes.add(listItem.toString().trim());
            }
        } else if (excludes instanceof String) {
            formattedExcludes.addAll(parseExcludesString((String) excludes));
        }
        return formattedExcludes;
    }

    private Set<String> parseExcludesString(String excludes) {
        Set<String> excludeSet = new HashSet<>();
        for (String exclude : excludes.split(",")) {
            String trimmedExclude = exclude.trim();
            if (!trimmedExclude.isEmpty()) {
                excludeSet.add(trimmedExclude);
            }
        }
        return excludeSet;
    }

    public Map<String, String> removeExcludedLabels(Map<String, String> labels) {
        Map<String, String> filteredLabels = new HashMap<>();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (!isExcluded(entry.getKey())) {
                filteredLabels.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredLabels;
    }

    public boolean isExcluded(String label) {
        return getExcludeSet().contains(label);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public Set<String> getExcludeSet() {
        return excludeSet;
    }

}
