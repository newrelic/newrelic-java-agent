package com.newrelic.agent.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApplicationLoggingLabelsConfig extends BaseConfig {
    public static final String ROOT = "labels";
    public static final String ENABLED = "enabled";
    public static final String EXCLUDE = "exclude";

    private final boolean enabled;
    private final Set<String> excludeSet;

    /**
     * Constructor to initialize LogLabelsConfig from configuration properties.
     *
     * @param props Map containing configuration properties.
     */
    public ApplicationLoggingLabelsConfig(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot + ROOT + ".");
        enabled = props.containsKey(ENABLED) && (boolean) props.get(ENABLED);
        excludeSet = initExcludes(props);
    }

    private Set<String> initExcludes(Map<String, Object> props) {
        List<String> excludeList = (List<String>) props.getOrDefault(EXCLUDE, Collections.emptyList());
        if (excludeList == null) {
            return Collections.emptySet();
        }
        Set<String> excludeSet = new HashSet<>();
        for (String exclude : excludeList) {
            String trimmedExclude = exclude.trim();
            if (!trimmedExclude.isEmpty()) {
                excludeSet.add(trimmedExclude);
            }
        }
        return excludeSet;
    }

    public boolean getEnabled() { return enabled; }

    public Set<String> getExcludeSet() { return excludeSet; }

    public boolean isExcluded(String label) { return getExcludeSet().contains(label); }

    public Map<String, String> removeExcludedLabels(Map<String, String> labels) {
        Map<String, String> filteredLabels = new HashMap<>();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (!isExcluded(entry.getKey())) {
                filteredLabels.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredLabels;
    }
}
