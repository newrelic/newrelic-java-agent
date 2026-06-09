/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.agent.instrumentation.solr;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpPrivateApi;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;

import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricsContext;
import org.apache.solr.store.blockcache.Metrics;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.solr.metrics" })
public class SolrMetricManagerInstrumentationTests {

    private SolrMetricManager solrMetricManager;
    private SolrMetricsContext solrMetricsContext;

    @Before
    public void before() {
        MetricUtil.clearRegistry("exampleRegistry");
        MetricUtil.clearRegistry("targetRegistry");

        // Initialize SolrMetricManager and create a metrics context for testing
        solrMetricManager = new SolrMetricManager();
        solrMetricsContext = new SolrMetricsContext(solrMetricManager, "exampleRegistry", "test-tag");

        // Initialize the SolrComponentRegistry for the new instrumentation approach
        SolrComponentRegistry.clear();
    }

    @Test
    public void register() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrInfoBean solrInfoBean = new Metrics();

        // Register the component to populate the tag->name mapping
        // This simulates what SolrCoreMetricManager_Instrumentation would do
        solrInfoBean.initializeMetrics(solrMetricsContext, "hdfsBlockCache");
        SolrMetricsContext childContext = solrInfoBean.getSolrMetricsContext();
        SolrComponentRegistry.registerComponent(childContext.getTag(), solrInfoBean.getName());

        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.registerMetric(
                childContext,                                    // SolrMetricsContext (not SolrInfoBean)
                "exampleRegistry",                               // registry name
                metricsMap,                                      // metric
                SolrMetricManager.ResolutionStrategy.REPLACE,    // strategy (not boolean)
                "filterCache",                                   // metricName
                "hdfsBlockCache"                                 // metricPath
        );

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream()
                .map(NRMetric::reportMetrics)
                .mapToInt(Integer::intValue)
                .sum();
        assertEquals(1, metricsReported);

        String registryName = MetricUtil.getMetrics().values().stream()
                .map(x -> x.registry)
                .collect(Collectors.joining());
        assertEquals("exampleRegistry", registryName);
    }

    @Test
    public void removeRegistry() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrInfoBean solrInfoBean = new Metrics();

        solrInfoBean.initializeMetrics(solrMetricsContext, "hdfsBlockCache");
        SolrMetricsContext childContext = solrInfoBean.getSolrMetricsContext();
        SolrComponentRegistry.registerComponent(childContext.getTag(), solrInfoBean.getName());

        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.registerMetric(
                childContext,
                "exampleRegistry",
                metricsMap,
                SolrMetricManager.ResolutionStrategy.REPLACE,
                "filterCache",
                "hdfsBlockCache"
        );
        solrMetricManager.removeRegistry("exampleRegistry");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream()
                .map(NRMetric::reportMetrics)
                .mapToInt(Integer::intValue)
                .sum();
        assertEquals(0, metricsReported);
    }

    @Test
    public void clearRegistry() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrInfoBean solrInfoBean = new Metrics();

        solrInfoBean.initializeMetrics(solrMetricsContext, "hdfsBlockCache");
        SolrMetricsContext childContext = solrInfoBean.getSolrMetricsContext();
        SolrComponentRegistry.registerComponent(childContext.getTag(), solrInfoBean.getName());

        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.registerMetric(
                childContext,
                "exampleRegistry",
                metricsMap,
                SolrMetricManager.ResolutionStrategy.REPLACE,
                "filterCache",
                "hdfsBlockCache"
        );
        solrMetricManager.clearRegistry("exampleRegistry");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream()
                .map(NRMetric::reportMetrics)
                .mapToInt(Integer::intValue)
                .sum();
        assertEquals(0, metricsReported);
    }

    @Test
    public void clearMetrics() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrInfoBean solrInfoBean = new Metrics();

        solrInfoBean.initializeMetrics(solrMetricsContext, "hdfsBlockCache");
        SolrMetricsContext childContext = solrInfoBean.getSolrMetricsContext();
        SolrComponentRegistry.registerComponent(childContext.getTag(), solrInfoBean.getName());

        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.registerMetric(
                childContext,
                "exampleRegistry",
                metricsMap,
                SolrMetricManager.ResolutionStrategy.REPLACE,
                "filterCache",
                "hdfsBlockCache"
        );
        solrMetricManager.clearMetrics("exampleRegistry", "hdfsBlockCache");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream()
                .map(NRMetric::reportMetrics)
                .mapToInt(Integer::intValue)
                .sum();
        assertEquals(0, metricsReported);
    }

    @Test
    public void swapRegistries() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrInfoBean solrInfoBean = new Metrics();

        solrInfoBean.initializeMetrics(solrMetricsContext, "hdfsBlockCache");
        SolrMetricsContext childContext = solrInfoBean.getSolrMetricsContext();
        SolrComponentRegistry.registerComponent(childContext.getTag(), solrInfoBean.getName());

        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.registerMetric(
                childContext,
                "exampleRegistry",
                metricsMap,
                SolrMetricManager.ResolutionStrategy.REPLACE,
                "filterCache",
                "hdfsBlockCache"
        );
        solrMetricManager.swapRegistries("exampleRegistry", "targetRegistry");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream()
                .map(NRMetric::reportMetrics)
                .mapToInt(Integer::intValue)
                .sum();
        assertEquals(1, metricsReported);

        String registryName = MetricUtil.getMetrics().values().stream()
                .map(x -> x.registry)
                .collect(Collectors.joining());
        assertEquals("targetRegistry", registryName);

        String metricBase = MetricUtil.getMetrics().values().stream()
                .map(NRMetric::getMetricBase)
                .collect(Collectors.joining());
        assertEquals("JMX/solr/targetRegistry/filterCache/hdfsBlockCache", metricBase);
    }
}
