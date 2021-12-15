/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.metrics;

import com.agent.instrumentation.solr.CacheMetric;
import com.agent.instrumentation.solr.GaugeMetric;
import com.agent.instrumentation.solr.MeteredMetric;
import com.agent.instrumentation.solr.MetricUtil;
import com.agent.instrumentation.solr.SolrMetricCollector;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.core.SolrInfoBean;

import java.util.logging.Level;

@Weave(originalName = "org.apache.solr.metrics.SolrMetricManager", type = MatchType.ExactClass)
public abstract class SolrMetricManager_Instrumentation {

    public void register(SolrInfoBean info, String registry, Metric metric, boolean force, String metricName, String... metricPath) {
        if (MetricUtil.isDesired(metricName, metricPath)) {
            SolrMetricCollector.initialize();

            boolean isCacheMetric = MetricsMap.class.isInstance(metric);
            boolean isGaugeMetric = Gauge.class.isInstance(metric);

            String desired = MetricUtil.getDesired(metricName, metricPath);

            if (isCacheMetric) {
                MetricsMap mMap = (MetricsMap) metric;
                CacheMetric nrMetric = new CacheMetric(desired, MetricUtil.getRegistry(registry), mMap, info.getName());
                NewRelic.getAgent().getLogger().log(Level.FINEST, "Created CacheMetric of name {0}", metricName);
                MetricUtil.addMetric(nrMetric);
            } else if (isGaugeMetric) {
                Gauge gauge = (Gauge) metric;
                GaugeMetric gMetric = new GaugeMetric(metricName, desired, MetricUtil.getRegistry(registry), gauge, info.getName());
                NewRelic.getAgent().getLogger().log(Level.FINEST, "Created GaugeMetric of name {0}", metricName);
                MetricUtil.addMetric(gMetric);
            }
        }

        Weaver.callOriginal();
    }

    public Meter meter(SolrInfoBean info, String registry, String metricName, String[] metricPath) {
        Meter meter = Weaver.callOriginal();
        if (MetricUtil.isDesired(metricName, metricPath)) {
            String mName = MetricUtil.getRemap(metricName);
            String desired = MetricUtil.getDesired(metricName, metricPath);
            MeteredMetric meteredMetric = new MeteredMetric(mName, desired, MetricUtil.getRegistry(registry), info.getName(), meter);
            MetricUtil.addMetric(meteredMetric);
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Added NRMetric from ({0}, {1}, {2})", info, registry, metricName);
        }
        return meter;
    }

}
