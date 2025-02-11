package com.newrelic.agent.config;

import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;

import java.util.Collections;
import java.util.HashMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.newrelic.agent.MetricNames.SUPPORTABILITY_LOGGING_LABELS_JAVA_ENABLED;
import static com.newrelic.agent.MetricNames.SUPPORTABILITY_LOGGING_LABELS_JAVA_DISABLED;

public class ApplicationLoggingLabelsConfig extends BaseConfig {
    public static final String ROOT = "labels";
    public static final String ENABLED = "enabled";
    public static final String EXCLUDE = "exclude";

    public static final boolean DEFAULT_ENABLED = false;

    public static boolean supportabilityMetricRecorded = false;
    private final boolean enabled;
    private final Map<String, String> labelsMap;
    private final Set<String> excludeSet;

    /**
     * Constructor to initialize LogLabelsConfig from configuration properties.
     *
     * @param props Map containing configuration properties.
     */
    public ApplicationLoggingLabelsConfig(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot + ROOT + ".");
        System.out.println("ApplicationLoggingLabelsConfig constructor: props = " + props);
        enabled = getProperty(ENABLED, DEFAULT_ENABLED);
        labelsMap = initLabels();
        System.out.println("ApplicationLoggingLabelsConfig constructor: labelsMap after init = " + labelsMap);
        excludeSet = initExclude();

        if (enabled && !supportabilityMetricRecorded) {
            System.out.println("ApplicationLoggingLabelsConfig constructor: labelsMap = " + labelsMap);
            System.out.println("ApplicationLoggingLabelsConfig constructor: excludeSet = " + excludeSet);
            sendSupportabilityMetric();
        } else {
            System.out.println("ApplicationLoggingLabelsConfig constructor: labelsMap = " + labelsMap);
            System.out.println("ApplicationLoggingLabelsConfig constructor: excludeSet = " + excludeSet);
            sendSupportabilityMetric();
        }
    }

    private void sendSupportabilityMetric() {
        if (enabled && !supportabilityMetricRecorded) {
            NewRelic.incrementCounter(SUPPORTABILITY_LOGGING_LABELS_JAVA_ENABLED);
            supportabilityMetricRecorded = true;
            System.out.println("ApplicationLoggingLabelsConfig constructor: Supportability Metric isEnabled sent.");
        } else if (enabled && supportabilityMetricRecorded) {
            System.out.println("ApplicationLoggingLabelsConfig constructor: Supportability Metric already sent.");
        } else {
            NewRelic.incrementCounter(SUPPORTABILITY_LOGGING_LABELS_JAVA_DISABLED);
            System.out.println("ApplicationLoggingLabelsConfig constructor: Supportability Metric isDisabled sent.");
        }
    }

    private void registerAgentConnectionListener() {
        ServiceFactory.getRPMServiceManager().addConnectionListener(new ConnectionListener() {
            @Override
            public void connected(IRPMService rpmService, AgentConfig agentConfig) {
                if (rpmService.isConnected() && !supportabilityMetricRecorded) {
                    System.out.println("ApplicationLoggingLabelsConfig constructor: Agent initialized.");
                    sendSupportabilityMetric();
                }
            }

            @Override
            public void disconnected(IRPMService rpmService) {
                supportabilityMetricRecorded = false;
            }
        });
    }

    /**
     * Initialize the log labels map
     *
     * @return A map of log labels
     */
    private Map<String, String> initLabels() {
        Map<String, String> parsedLabels = new HashMap<>();
        Map<String, String> rawLabels = getProperty("labels");

        if (rawLabels != null) {
            for (Map.Entry<String, String> entry : rawLabels.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    if (isExcluded(entry.getKey())) {
                        continue;
                    }
                    parsedLabels.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(parsedLabels);
    }

    /**
     * Initialize the set of keys to exclude from log labels
     *
     * @return A set of keys to exclude from log labels
     */
    private Set<String> initExclude() {
//        Set<String> excludes = getProperty(EXCLUDE, Collections.emptySet());
//        if (excludes == null) {
//            return Collections.emptySet();
//        }

        return new HashSet<>(getUniqueStrings(EXCLUDE));


    }


    public boolean getEnabled() {
        return enabled;
    }

    public Map<String, String> getLogLabels() {
        return labelsMap;
    }

    public Set<String> getExcludeSet() {
        return excludeSet;
    }

    public boolean isExcluded(String label) {
        return false;
//        return getExcludeSet().contains(label);
    }

}
