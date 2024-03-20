/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.stats.StatsEngine;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Used for attributes where we want to get their values.
 */
public abstract class JmxGet extends JmxObject {

    private static final Pattern TYPE_QUERY_PATTERN = Pattern.compile(",(.*?)=");
    private static final Pattern PULL_VALUE_PATTERN = Pattern.compile("\\{(.*?)\\}");
    private static final Pattern PULL_ATTRIBUTE_PATTERN = Pattern.compile("\\:(.*?)\\:");
    private static final Pattern PULL_ITER_VAL_PATTERN = Pattern.compile("for\\:(.*)\\[([0-9]+)\\:([0-9]*)\\].*");
    private static final Pattern RANGE_PATTERN = Pattern.compile("\\[([0-9]+)\\:([0-9]*)\\]");

    /** This should be everything but the attribute portion of the metric. */
    private final String rootMetricName;
    private final boolean isPattern;

    private final Set<String> attributes;
    private final List<JmxMetric> metrics;
    private final Extension origin;
    private final JmxAttributeFilter attributeFilter;
    private final JmxMetricModifier modifier;

    /**
     * 
     * Creates this JmxGet.
     * 
     * @param pObjectName The object name.
     * @param safeName The safeName for the metric.
     * @param pAttributesToType The attributes corresponding with the type.
     * @throws MalformedObjectNameException Thrown if a problem with the object name.
     */
    public JmxGet(String pObjectName, String rootMetricName, String safeName,
            Map<JmxType, List<String>> pAttributesToType, Extension origin) throws MalformedObjectNameException {
        super(pObjectName, safeName);
        this.origin = origin;
        this.attributeFilter = null;
        modifier = null;
        this.rootMetricName = getRootMetricName(rootMetricName);
        isPattern = isPattern(rootMetricName);
        attributes = new HashSet<>();
        metrics = new ArrayList<>();

        // add metrics and attributes
        for (Entry<JmxType, List<String>> current : pAttributesToType.entrySet()) {
            JmxType type = current.getKey();
            List<String> attrs = current.getValue();
            for (String att : attrs) {
                attributes.add(att);
                metrics.add(JmxMetric.create(att, type));
            }
        }
    }

    /**
     * 
     * Creates this JmxGet.
     */
    public JmxGet(String pObjectName, String safeName, String pRootMetric, List<JmxMetric> pMetrics,
            JmxAttributeFilter attributeFilter, JmxMetricModifier pModifier) throws MalformedObjectNameException {
        super(pObjectName, safeName);
        this.origin = null;
        this.attributeFilter = attributeFilter;
        modifier = pModifier;

        rootMetricName = getRootMetricName(pRootMetric);
        isPattern = isPattern(rootMetricName);

        if (pMetrics == null) {
            metrics = new ArrayList<>();
        } else {
            metrics = pMetrics;
        }

        attributes = new HashSet<>();
        // add all of the attributes
        for (JmxMetric m : metrics) {
            attributes.addAll(Arrays.asList(m.getAttributes()));
        }
    }

    public abstract void recordStats(StatsEngine statsEngine,
            Map<ObjectName, Map<String, Float>> resultingMetricToValue, MBeanServer server);

    private static boolean isPattern(String rootMetricName) {
        if (rootMetricName != null) {
            return rootMetricName.contains("{") ? true : false;
        } else {
            return false;
        }
    }

    private String getRootMetricName(String root) {
        if (root != null) {
            if (!root.endsWith("/")) {
                root = root + "/";
            }
            if (!(root.startsWith(MetricNames.JMX_WITH_SLASH) || root.startsWith(MetricNames.JMX_CUSTOM))) {
                root = MetricNames.JMX_WITH_SLASH + root;
            }
        }
        return root;
    }

    public Collection<String> getAttributes() {
        return attributes;
    }

    public String getRootMetricName(ObjectName actualName, MBeanServer server) {
        if (rootMetricName != null) {
            return pullAttValuesFromName(actualName, server);
        }

        return getDefaultName(actualName);
    }

    private String pullAttValuesFromName(ObjectName actualName, MBeanServer server) {
        if (!isPattern) {
            return rootMetricName;
        }

        StringBuffer sb = new StringBuffer();
        Matcher m = PULL_VALUE_PATTERN.matcher(rootMetricName);
        Map<String, String> keyProperties = actualName.getKeyPropertyList();
        String key;
        String value = null;
        while (m.find()) {

            key = m.group(1);

            String iteratedValues = null;
            try {
                iteratedValues = matchAndGetIteratedValue(key, keyProperties, actualName, server);
            }  catch (Throwable e) {
                Agent.LOG.log(Level.FINEST, e, e.getMessage());
            }

            if (iteratedValues != null) {
                value = iteratedValues;
            } else {
                value = getValueFromMBeanKey(key, keyProperties, actualName, server);
            }


            if (value != null) {
                m.appendReplacement(sb, cleanValue(value));
            } else {
                m.appendReplacement(sb, "");
            }
        }
        m.appendTail(sb);

        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }

        if (modifier == null) {
            return sb.toString();
        } else {
            return modifier.getMetricName(sb.toString());
        }
    }

    private String matchAndGetIteratedValue(String key, Map<String, String> keyProperties,
            ObjectName actualName, MBeanServer server) {

        Matcher iterMatcher = PULL_ITER_VAL_PATTERN.matcher(key);
        if (!iterMatcher.matches())  {
            return null;
        }

        String rangedKeyFormat = key.substring(4);
        Matcher rangeMatcher = RANGE_PATTERN.matcher(rangedKeyFormat);

        if (!rangeMatcher.find()) {
            return null;
        }
        String rangeStartStr = rangeMatcher.group(1);
        String rangeEndStr = rangeMatcher.group(2);
        rangeMatcher.reset();

        int rangeStart = (rangeStartStr != null && !rangeStartStr.isEmpty()) ? Integer.parseInt(rangeStartStr): 0;
        int rangeEnd = (rangeEndStr != null && !rangeEndStr.isEmpty())
                ? Integer.parseInt(rangeEndStr)
                : rangeStart + keyProperties.size();

        List<String> valueSequence = new ArrayList<>(keyProperties.size());

        for (int i = rangeStart; i < rangeEnd && rangeMatcher.find(); i++) {
            StringBuffer sb = new StringBuffer(key.length());
            rangeMatcher.appendReplacement(sb, Integer.toString(i));
            rangeMatcher.appendTail(sb);
            rangeMatcher.reset();

            String mbeanKey = sb.toString();
            String value = getValueFromMBeanKey(mbeanKey, keyProperties, actualName, server);
            if (value != null) {
                valueSequence.add(value);
            }
        }

        return String.join("/", valueSequence);
    }

    private String getValueFromMBeanKey(String key, Map<String, String> keyProperties,
            ObjectName actualName, MBeanServer server) {
        String value = null;
        Matcher pullAttrMatcher = PULL_ATTRIBUTE_PATTERN.matcher(key);
        if (pullAttrMatcher.matches()) {
            key = pullAttrMatcher.group(1);
            try {
                value = server.getAttribute(actualName, key).toString();
            } catch (Throwable e) {
                Agent.LOG.log(Level.FINEST, e, e.getMessage());
            }
        } else {
            value = keyProperties.get(key);
        }
        return value;
    }

    protected static String cleanValue(String value) {
        // remove leading slash
        value = value.trim();
        if (value.length() > 0 && value.charAt(0) == '/') {
            return value.substring(1);
        }
        return value;
    }

    private String getDefaultName(ObjectName actualName) {
        Map<String, String> keyProperties = actualName.getKeyPropertyList();
        String type = keyProperties.remove("type");
        StringBuilder rootPath = new StringBuilder(MetricNames.JMX).append('/');
        if (actualName.getDomain() != null) {
            rootPath.append(actualName.getDomain()).append('/');
        }
        rootPath.append(type);

        if (keyProperties.size() > 1) {
            String str = getObjectNameString();
            Matcher matcher = TYPE_QUERY_PATTERN.matcher(str);

            while (matcher.find()) {
                String group = matcher.group(1);
                String val = keyProperties.remove(group);
                if (val != null) {
                    rootPath.append('/');
                    rootPath.append(formatSegment(val));
                }
            }
        }
        if (keyProperties.size() == 1) {
            rootPath.append('/');
            rootPath.append(formatSegment(keyProperties.entrySet().iterator().next().getValue()));
        }
        rootPath.append('/');
        return rootPath.toString();
    }

    private static String formatSegment(String metricSegment) {
        if ((metricSegment.length() > 0) && (metricSegment.charAt(0) == '/')) {
            return metricSegment.substring(1);
        }
        return metricSegment;
    }

    /**
     * String representation of this JmxGet.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("object_name: ").append(getObjectNameString());
        sb.append(" attributes: [");
        Iterator<JmxMetric> it = metrics.iterator();
        while (it.hasNext()) {
            JmxMetric metric = it.next();
            sb.append(metric.getAttributeMetricName()).append(" type: ").append(metric.getType().getYmlName());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public Extension getOrigin() {
        return origin;
    }

    protected JmxAttributeFilter getJmxAttributeFilter() {
        return attributeFilter;
    }

    protected List<JmxMetric> getJmxMetrics() {
        return metrics;
    }

}
