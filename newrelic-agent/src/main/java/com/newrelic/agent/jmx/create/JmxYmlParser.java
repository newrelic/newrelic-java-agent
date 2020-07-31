/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import com.newrelic.agent.Agent;
import com.newrelic.agent.jmx.JmxType;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class JmxYmlParser implements JmxConfiguration {
    private static final String YML_PROP_OBJECT_NAME = "object_name";
    private static final String YML_PROP_ROOT_METRIC_NAME = "root_metric_name";
    private static final String YML_PROP_ENABLED = "enabled";
    private static final String YML_PROP_METRICS = "metrics";
    private static final String YML_PROP_ATTRS = "attributes";
    private static final String YML_PROP_ATT = "attribute";
    private static final String YML_PROP_TYPE = "type";

    private final Map<?, ?> jmxConfig;

    /**
     * 
     * Creates this JmxYmlUtils.
     */
    public JmxYmlParser(Map<?, ?> pJmxConfig) {
        super();
        jmxConfig = pJmxConfig;
    }

    /**
     * Gets the object name from the JMX configuration.
     * 
     * @return The object name as a string.
     */
    @Override
    public String getObjectName() {
        return (String) jmxConfig.get(YML_PROP_OBJECT_NAME);
    }

    @Override
    public String getRootMetricName() {
        return (String) jmxConfig.get(YML_PROP_ROOT_METRIC_NAME);
    }

    /**
     * Gets the enabled flag from the JMX configuration.
     * 
     * @return True if the property is not present of is set to true, else false.
     */
    @Override
    public boolean getEnabled() {
        Boolean isEnabled = (Boolean) jmxConfig.get(YML_PROP_ENABLED);
        return (isEnabled == null) || isEnabled;
    }

    /**
     * Parses the JMX configuration and returns a map of JMX type to attributes.
     * 
     * @return Map of JMX types to attributes listed in the JMX configuration.
     */
    @Override
    public Map<JmxType, List<String>> getAttrs() {
        Object metrics = jmxConfig.get(YML_PROP_METRICS);
        if (metrics == null) {
            Agent.LOG.log(Level.WARNING,
                    "There is no 'metric' property in the JMX configuration file. Please verify the format of your yml file.");
            return null;
        } else if (!(metrics instanceof List)) {
            Agent.LOG.log(Level.WARNING,
                    "The 'metric' property in the JMX configuration file is incorrect. Please verify the format of your yml file.");
            return null;
        } else {
            // at most two objects should be put in here meaning 3 is good size
            Map<JmxType, List<String>> attrs = new HashMap<>(3);

            for (Map<?, ?> metric : (List<Map>) metrics) {
                JmxType type = findType(metric);
                List<String> attList = findAttributes(metric);
                if (attList.size() > 0) {
                    List<String> alreadyAdded = attrs.get(type);
                    if (alreadyAdded == null) {
                        attrs.put(type, attList);
                    } else {
                        alreadyAdded.addAll(attList);
                    }
                }
            }
            return attrs;
        }
    }

    private JmxType findType(Map<?, ?> metricMap) {
        String type = (String) metricMap.get(YML_PROP_TYPE);
        if (type == null || type.equals(JmxType.MONOTONICALLY_INCREASING.getYmlName())) {
            return JmxType.MONOTONICALLY_INCREASING;
        } else if (type.equals(JmxType.SIMPLE.getYmlName())) {
            return JmxType.SIMPLE;
        } else {
            String msg = MessageFormat.format("Unknown JMX metric type: {0}.  Using default type: {1}", type,
                    JmxType.MONOTONICALLY_INCREASING);
            Agent.LOG.warning(msg);
            return JmxType.MONOTONICALLY_INCREASING;
        }
    }

    private List<String> findAttributes(Map<?, ?> metricMap) {
        List<String> result = new ArrayList<>();
        String attributes = (String) metricMap.get(YML_PROP_ATTRS);
        if (attributes != null) {
            String current;
            for (String attribute : attributes.split(",")) {
                current = attribute.trim();
                if (current.length() != 0) {
                    result.add(current);
                }
            }
        } else {
            String attribute = (String) metricMap.get(YML_PROP_ATT);
            if ((attribute != null) && (attribute.trim().length() > 0)) {
                result.add(attribute.trim());
            }
        }
        return result;
    }
}
