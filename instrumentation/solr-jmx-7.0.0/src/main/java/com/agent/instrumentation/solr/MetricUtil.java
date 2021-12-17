/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class MetricUtil {

    private static List<String> desiredMetrics = new ArrayList<>();

    private static List<String> desiredPaths = new ArrayList<>();

    private static ConcurrentHashMap<String, NRMetric> metrics = new ConcurrentHashMap<>();

    private static final String REGISTRY_PREFIX = "solr.core";

    private static final HashMap<String, String> remaps = new HashMap<>();

    static {
        desiredMetrics.add("filterCache");
        desiredMetrics.add("queryResultCache");
        desiredMetrics.add("documentCache");
        desiredPaths.add("updateHandler");
        remaps.put("cumulativeAdds", "cumulative_adds");
        remaps.put("cumulativeDeletesById", "cumulative_deletesById");
        remaps.put("cumulativeDeletesByQuery", "cumulative_deletesByQuery");
        remaps.put("cumulativeErrors", "cumulative_errors");
    }

    public static String getRemap(String key) {
        if (remaps.containsKey(key)) {
            return remaps.get(key);
        }
        return key;
    }

    public static void addMetric(NRMetric metric) {
        String metricBase = metric.getMetricBase();
        metrics.put(metricBase, metric);
    }

    public static void removeMetric(String registry, String... metricPath) {
        metrics.entrySet()
                .stream()
                .filter(entry -> entry.getValue().registry.equals(registry) && Arrays.stream(metricPath).anyMatch(path -> path.startsWith(entry.getValue().name)))
                .forEach(x -> metrics.remove(x.getKey()));
    }

    public static void swapRegistries(String sourceRegistry, String targetRegistry) {
        metrics.entrySet()
                .stream()
                .filter(entry -> entry.getValue().registry.equals(getRegistry(sourceRegistry)))
                .forEach(x -> {
                    String currentKey = x.getKey();
                    NRMetric metric = x.getValue();
                    metric.setRegistry(getRegistry(targetRegistry));
                    addMetric(metric);
                    metrics.remove(currentKey);
                });
    }

    public static void clearRegistry(String registry) {
        metrics.entrySet()
                .stream()
                .filter(entry -> entry.getValue().registry.equals(registry))
                .forEach(x -> metrics.remove(x.getKey()));
    }

    public static String getRegistry(String r) {
        if (r.startsWith(REGISTRY_PREFIX)) {
            return r.substring(REGISTRY_PREFIX.length() + 1);
        } else {
            return r;
        }
    }

    public static ConcurrentHashMap<String, NRMetric> getMetrics() {
        return metrics;
    }

    public static String getDesired(String metricName, String[] metricPath) {
        if (!isDesired(metricName, metricPath)) {
            return null;
        }
        if (metricName != null && !metricName.isEmpty()) {
            StringTokenizer st = new StringTokenizer(metricName, ".");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (desiredMetrics.contains(token)) {
                    return token;
                }
            }
        }
        if (metricPath != null) {
            for (int i = 0; i < metricPath.length; i++) {
                if (desiredPaths.contains(metricPath[i])) {
                    return metricPath[i];
                }
            }
        }
        return null;
    }

    public static boolean isDesired(String metricName, String[] metricPath) {
        if (metricName != null && !metricName.isEmpty()) {
            StringTokenizer st = new StringTokenizer(metricName, ".");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (desiredMetrics.contains(token)) {
                    return true;
                }
            }
        }
        if (metricPath != null) {
            for (int i = 0; i < metricPath.length; i++) {
                if (desiredPaths.contains(metricPath[i])) {
                    return true;
                }
            }
        }
        return false;
    }

}
