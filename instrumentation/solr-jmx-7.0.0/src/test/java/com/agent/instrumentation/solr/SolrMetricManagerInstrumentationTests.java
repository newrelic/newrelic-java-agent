package com.agent.instrumentation.solr;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpPrivateApi;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;

import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.store.blockcache.Metrics;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.solr.metrics" })
public class SolrMetricManagerInstrumentationTests {

    @Before
    public void before() {
        MetricUtil.clearRegistry("exampleRegistry");
        MetricUtil.clearRegistry("targetRegistry");
    }

    @Test
    public void register() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrMetricManager solrMetricManager = new SolrMetricManager();
        SolrInfoBean solrInfoBean = new Metrics();
        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.register(solrInfoBean, "exampleRegistry", metricsMap, false,"filterCache", "hdfsBlockCache");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream().map(NRMetric::reportMetrics).mapToInt(Integer::intValue).sum();
        assertEquals(1, metricsReported);

        String registryName = MetricUtil.getMetrics().values().stream().map(x -> x.registry).collect(Collectors.joining());
        assertEquals("exampleRegistry", registryName);
    }

    @Test
    public void removeRegistry() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrMetricManager solrMetricManager = new SolrMetricManager();
        SolrInfoBean solrInfoBean = new Metrics();
        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.register(solrInfoBean, "exampleRegistry", metricsMap, false,"filterCache", "hdfsBlockCache");
        solrMetricManager.removeRegistry("exampleRegistry");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream().map(NRMetric::reportMetrics).mapToInt(Integer::intValue).sum();
        assertEquals(0, metricsReported);
    }

    @Test
    public void clearRegistry() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrMetricManager solrMetricManager = new SolrMetricManager();
        SolrInfoBean solrInfoBean = new Metrics();
        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.register(solrInfoBean, "exampleRegistry", metricsMap, false,"filterCache", "hdfsBlockCache");
        solrMetricManager.clearRegistry("exampleRegistry");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream().map(NRMetric::reportMetrics).mapToInt(Integer::intValue).sum();
        assertEquals(0, metricsReported);
    }

    @Test
    public void clearMetrics() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrMetricManager solrMetricManager = new SolrMetricManager();
        SolrInfoBean solrInfoBean = new Metrics();
        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.register(solrInfoBean, "exampleRegistry", metricsMap, false,"filterCache", "hdfsBlockCache");
        solrMetricManager.clearMetrics("exampleRegistry", "hdfsBlockCache");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream().map(NRMetric::reportMetrics).mapToInt(Integer::intValue).sum();
        assertEquals(0, metricsReported);
    }

    @Test
    public void swapRegistries() {
        //Given
        AgentBridge.privateApi = new NoOpPrivateApi();
        SolrMetricManager solrMetricManager = new SolrMetricManager();
        SolrInfoBean solrInfoBean = new Metrics();
        MetricsMap metricsMap = new MetricsMap((detailed, map) -> map.put("exampleMetric", 100));

        //When
        solrMetricManager.register(solrInfoBean, "exampleRegistry", metricsMap, false,"filterCache", "hdfsBlockCache");
        solrMetricManager.swapRegistries("exampleRegistry", "targetRegistry");

        //Then
        int metricsReported = MetricUtil.getMetrics().values().stream().map(NRMetric::reportMetrics).mapToInt(Integer::intValue).sum();
        assertEquals(1, metricsReported);

        String registryName = MetricUtil.getMetrics().values().stream().map(x -> x.registry).collect(Collectors.joining());
        assertEquals("targetRegistry", registryName);
    }
}
