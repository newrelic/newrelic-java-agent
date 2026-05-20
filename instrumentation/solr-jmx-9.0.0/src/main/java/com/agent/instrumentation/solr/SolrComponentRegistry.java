/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.agent.instrumentation.solr;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Our instrumentation point for Solr v9 no longer accepts a SolrInfoBean instance
 * in the SolrMetricManager_Instrumentation.registerMetric method. Instead, it takes
 * a SolrMetricsContext instance, which no longer exposes a getName method, which
 * we use to generate metric names. This class is used to map getTag() values
 * (from SolrMetricsContext) to corresponding SolrInfoBean.getName() values.
 * This allows Solr 9.0 instrumentation to maintain metric name parity with Solr 8.x.
 */
public class SolrComponentRegistry {

    private static final ConcurrentHashMap<String, String> tagToName = new ConcurrentHashMap<>();

    /**
     * Register a component's context tag to bean name mapping.
     * If the name looks like a fully qualified class name, also updates any existing
     * metrics that are currently using a fallback name and have a matching context tag.
     *
     * @param tag The context tag
     * @param name The bean name from SolrInfoBean.getName()
     */
    public static void registerComponent(String tag, String name) {
        if (tag != null && name != null) {
            String previousName = tagToName.put(tag, name);

            // If this looks like a fully qualified class name and it's a new/updated mapping,
            // update existing metrics that might be using a fallback name
            if (isFullyQualifiedClassName(name) && !name.equals(previousName)) {
                updateMetricsWithBeanName(tag, name);
            }
        }
    }

    /**
     * Get the bean name for a given tag.
     *
     * @param tag The target tag
     * @return The bean name, or null if not found
     */
    public static String getNameForTag(String tag) {
        return tagToName.get(tag);
    }

    /**
     * Remove a tag --> name mapping
     *
     * @param tag The tag to remove
     */
    public static void removeComponent(String tag) {
        if (tag != null) {
            tagToName.remove(tag);
        }
    }

    /**
     * Clear everything
     */
    public static void clear() {
        tagToName.clear();
    }

    /**
     * Super simple method to check if a name looks like a fully qualified class name
     * (contains a dot and starts with lowercase package).
     *
     * @param name The name to check
     * @return true if it looks like a fully qualified class name
     */
    private static boolean isFullyQualifiedClassName(String name) {
        return name != null && name.length() > 0 && name.contains(".") && Character.isLowerCase(name.charAt(0));
    }

    /**
     * Update existing metrics that are using a fallback name (e.g., "updateHandler")
     * to use the proper fully qualified bean name. Only updates metrics with a matching context tag.
     *
     * @param tag The context tag to match
     * @param beanName The fully qualified bean name to use
     */
    private static void updateMetricsWithBeanName(String tag, String beanName) {
        java.util.concurrent.ConcurrentHashMap<String, NRMetric> metrics = MetricUtil.getMetrics();

        for (NRMetric metric : metrics.values()) {
            // Only update metrics that:
            // Have a matching context tag
            // Are using a simple fallback name (doesn't contain dots)
            // Don't already have the target bean name
            String currentName = metric.getName();
            String metricTag = metric.getContextTag();

            if (tag != null && tag.equals(metricTag) &&
                    currentName != null && !currentName.contains(".") && !beanName.equals(currentName)) {
                metric.setName(beanName);
            }
        }
    }

}
