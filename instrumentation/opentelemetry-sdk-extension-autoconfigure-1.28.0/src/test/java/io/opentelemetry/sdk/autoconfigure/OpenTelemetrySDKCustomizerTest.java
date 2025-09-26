/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.config.OtelConfig;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.opentelemetry.sdk.autoconfigure.OpenTelemetrySDKCustomizer.SERVICE_INSTANCE_ID_ATTRIBUTE_KEY;
import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.DELTA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenTelemetrySDKCustomizerTest extends TestCase {

    public void testApplyProperties() {
        Agent agent = mock(Agent.class);
        Logger logger = mock(Logger.class);
        when(agent.getLogger()).thenReturn(logger);
        Config config = mock(Config.class);
        when(agent.getConfig()).thenReturn(config);
        when(config.getValue("app_name")).thenReturn("Test");
        when(config.getValue("host")).thenReturn("mylaptop");
        when(config.getValue("license_key")).thenReturn("12345");

        Map<String, String> properties = OpenTelemetrySDKCustomizer.applyProperties(mock(ConfigProperties.class), agent);
        assertEquals("api-key=12345", properties.get("otel.exporter.otlp.headers"));
        assertEquals("https://mylaptop:443", properties.get("otel.exporter.otlp.endpoint"));
        assertEquals("http/protobuf", properties.get("otel.exporter.otlp.protocol"));
        assertEquals("Test", properties.get("otel.service.name"));
        assertEquals("gzip", properties.get("otel.exporter.otlp.compression"));
    }

    public void testApplyResourcesServiceInstanceIdSet() {
        com.newrelic.agent.bridge.Agent agent = mock(com.newrelic.agent.bridge.Agent.class);
        Resource resource = OpenTelemetrySDKCustomizer.applyResources(
                Resource.create(Attributes.of(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY, "7fjjr")), agent, mock(Logger.class));
        assertEquals("7fjjr", resource.getAttribute(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY));
        assertNull(resource.getAttribute(AttributeKey.stringKey("entity.guid")));
    }

    public void testApplyResources() {
        com.newrelic.agent.bridge.Agent agent = mock(com.newrelic.agent.bridge.Agent.class);
        when(agent.getEntityGuid(true)).thenReturn("myguid");
        Resource resource = OpenTelemetrySDKCustomizer.applyResources(
                Resource.empty(), agent, mock(Logger.class));
        assertNotNull(resource.getAttribute(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY));
        assertEquals("myguid", resource.getAttribute(AttributeKey.stringKey("entity.guid")));
    }

    public void testApplyMeterExcludesDropsExcludedMeters() {
        DummyExporter metricExporter = new DummyExporter();
        MetricReader reader = PeriodicMetricReader.create(metricExporter);
        List<String> expectedExclude = Arrays.asList("drop-me", "drop-me-too", "never-used");

        SdkMeterProvider provider = setupProviderFromExcludesConfig(reader, expectedExclude);

        //produce to some meters and force them to be exported
        provider.get("drop-me").counterBuilder("foo").build().add(1);
        provider.get("drop-me-too").counterBuilder("bar").build().add(2);
        provider.get("keep-me").counterBuilder("baz").build().add(3);
        provider.get("keep-me").counterBuilder("hello").build().add(3);
        reader.forceFlush();

        Collection<MetricData> collectedMetrics = metricExporter.getLatestMetricData();
        List<String> collectedMetricNames = metricNames(collectedMetrics);

        assertEquals(2, collectedMetricNames.size());
        assertFalse(collectedMetricNames.contains("foo"));
        assertFalse(collectedMetricNames.contains("bar"));
        assertTrue(collectedMetricNames.contains("baz"));
        assertTrue(collectedMetricNames.contains("hello"));
    }

    public void testApplyMeterExcludesDropsNothingWhenEmpty(){
        DummyExporter metricExporter = new DummyExporter();
        MetricReader reader = PeriodicMetricReader.create(metricExporter);
        List<String> noExcludedMeters = new ArrayList<>();
        SdkMeterProvider provider = setupProviderFromExcludesConfig(reader, noExcludedMeters);

        //produce to some meters and force them to be exported
        provider.get("keep-me").counterBuilder("foo").build().add(1);
        provider.get("keep-me-too").counterBuilder("bar").build().add(3);
        reader.forceFlush();

        Collection<MetricData> collectedMetrics = metricExporter.getLatestMetricData();
        List<String> actualMetricNames = metricNames(collectedMetrics);

        assertEquals(2, collectedMetrics.size());
        assertTrue(actualMetricNames.contains("foo"));
        assertTrue(actualMetricNames.contains("bar"));
    }
    
    private List<String> metricNames(Collection<MetricData> collectedMetrics) {
        List<String> metricNames = new ArrayList<>();
        for (MetricData metricData : collectedMetrics) {
            metricNames.add(metricData.getName());
        }
        return metricNames;
    }

    private SdkMeterProvider setupProviderFromExcludesConfig(MetricReader reader, List<String> excludedMeters) {
        Agent agent = mock(Agent.class);
        Logger logger = mock(Logger.class);
        when(agent.getLogger()).thenReturn(logger);
        OtelConfig otelConfig = mock(OtelConfig.class);
        when(otelConfig.getExcludedMeters()).thenReturn(excludedMeters);
        SdkMeterProviderBuilder customizedBuilder = OpenTelemetrySDKCustomizer.applyMeterExcludes(
                SdkMeterProvider.builder().registerMetricReader(reader), agent, otelConfig
        );
        return customizedBuilder.build();
    }

    // A dummy exporter exposes the last round of collected metrics on its result field.
    static class DummyExporter implements MetricExporter {

        //expose the most recently exported metrics
        private Collection<MetricData> lastestMetricData = null;

        //this is required to register views without throwing an exception
        @Override
        public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
            return Aggregation.sum();
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return DELTA;
        }

        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
            this.lastestMetricData = metrics;
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return null;
        }

        @Override
        public CompletableResultCode shutdown() {
            return null;
        }

        public Collection<MetricData> getLatestMetricData(){
            return lastestMetricData;
        }
    }
}