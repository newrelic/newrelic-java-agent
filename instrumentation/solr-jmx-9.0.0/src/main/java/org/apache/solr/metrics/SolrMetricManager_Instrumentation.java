/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.metrics;

import com.agent.instrumentation.solr.CacheMetric;
import com.agent.instrumentation.solr.GaugeMetric;
import com.agent.instrumentation.solr.MeteredMetric;
import com.agent.instrumentation.solr.MetricUtil;
import com.agent.instrumentation.solr.SolrComponentRegistry;
import com.agent.instrumentation.solr.SolrMetricCollector;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.Set;
import java.util.logging.Level;

@Weave(originalName = "org.apache.solr.metrics.SolrMetricManager", type = MatchType.ExactClass)
public abstract class SolrMetricManager_Instrumentation {

    public void registerMetric(SolrMetricsContext context, String registry, Metric metric,
            SolrMetricManager.ResolutionStrategy strategy, String metricName, String... metricPath) {
        if (MetricUtil.isDesired(metricName, metricPath)) {
            SolrMetricCollector.initialize();

            boolean isCacheMetric = metric instanceof MetricsMap;
            boolean isGaugeMetric = metric instanceof Gauge;

            String desired = MetricUtil.getDesired(metricName, metricPath);

            String tag = context != null ? context.getTag() : null;
            String beanName = getBeanName(context, desired);

            if (isCacheMetric) {
                MetricsMap mMap = (MetricsMap) metric;
                CacheMetric nrMetric = new CacheMetric(desired, MetricUtil.getRegistry(registry), mMap, beanName, tag);
                NewRelic.getAgent().getLogger().log(Level.FINEST, "Created CacheMetric of name {0}", metricName);
                MetricUtil.addMetric(nrMetric);
            } else if (isGaugeMetric) {
                Gauge gauge = (Gauge) metric;
                GaugeMetric gMetric = new GaugeMetric(metricName, desired, MetricUtil.getRegistry(registry), gauge, beanName, tag);
                NewRelic.getAgent().getLogger().log(Level.FINEST, "Created GaugeMetric of name {0}", metricName);
                MetricUtil.addMetric(gMetric);
            }
        }

        Weaver.callOriginal();
    }

    public Meter meter(SolrMetricsContext context, String registry, String metricName, String[] metricPath) {
        Meter meter = Weaver.callOriginal();
        if (MetricUtil.isDesired(metricName, metricPath)) {
            String mName = MetricUtil.getRemap(metricName);
            String desired = MetricUtil.getDesired(metricName, metricPath);
            String tag = context != null ? context.getTag() : null;
            String beanName = getBeanName(context, desired);
            MeteredMetric meteredMetric = new MeteredMetric(mName, desired, MetricUtil.getRegistry(registry), beanName, meter, tag);
            MetricUtil.addMetric(meteredMetric);
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Added NRMetric from ({0}, {1}, {2})", context, registry, metricName);
        }
        return meter;
    }

    public void removeRegistry(String registry) {
        Weaver.callOriginal();
        MetricUtil.clearRegistry(registry);
        NewRelic.getAgent().getLogger().log(Level.FINEST, "Removed {0} metric registry", registry);
    }

    public void clearRegistry(String registry) {
        Weaver.callOriginal();
        MetricUtil.clearRegistry(registry);
        NewRelic.getAgent().getLogger().log(Level.FINEST, "Cleared {0} metric registry", registry);
    }

    public Set<String> clearMetrics(String registry, String... metricPath) {
        Set<String> removedMetrics = Weaver.callOriginal();
        if(removedMetrics != null) {
            for (String removedMetric: removedMetrics) {
                MetricUtil.removeMetric(registry, removedMetric);
            }
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Cleared {0} metrics from {1} metric registry", removedMetrics.size(), registry);
        }
        return removedMetrics;
    }

    public void swapRegistries(String registry1, String registry2) {
        Weaver.callOriginal();
        MetricUtil.swapRegistries(registry1, registry2);
        NewRelic.getAgent().getLogger().log(Level.FINEST, "Swapped {0} metric registry to {1} metric registry", registry1, registry2);
    }

    /**
     * Resolves a bean name from a SolrMetricsContext by looking up the context's tag
     * in the SolrComponentRegistry. Falls back to the desired value if no mapping exists.
     *
     * @param context The SolrMetricsContext containing the tag to look up
     * @param desired The fallback value to use if no mapping is found
     * @return The resolved bean name, or the desired value if no mapping exists
     */
    private String getBeanName(SolrMetricsContext context, String desired) {
        String beanName = null;
        if (context != null) {
            String tag = context.getTag();
            beanName = SolrComponentRegistry.getNameForTag(tag);
        }

        // Fallback if no mapping exists
        if (beanName == null) {
            beanName = desired;
        }

        return beanName;
    }
}
