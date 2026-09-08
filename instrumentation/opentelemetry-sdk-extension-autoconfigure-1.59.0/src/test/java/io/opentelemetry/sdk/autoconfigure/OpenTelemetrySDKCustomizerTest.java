/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import com.nr.agent.instrumentation.utils.audit.OtlpAuditLogger;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.common.export.ProxyOptions;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.DEFAULT_PROXY_PORT;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_METRICS_EXCLUDE;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.PROXY_HOST;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.PROXY_PORT;
import static io.opentelemetry.sdk.autoconfigure.OpenTelemetrySDKCustomizer.SERVICE_INSTANCE_ID_ATTRIBUTE_KEY;
import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.DELTA;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        // This OTel version reads otel.java.exporter.otlp.retry.disabled, and retry is on by default,
        // so "false" is a no-op; otel.experimental.exporter.otlp.retry.enabled is set only so both
        // autoconfigure modules stay near-identical.
        assertEquals("true", properties.get("otel.experimental.exporter.otlp.retry.enabled"));
        assertEquals("false", properties.get("otel.java.exporter.otlp.retry.disabled"));
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

        SdkMeterProvider provider = setupProviderFromExcludesConfig(reader, "drop-me,drop-me-too,never-used");

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

    public void testApplyMeterExcludesDropsNothingWhenEmpty() {
        DummyExporter metricExporter = new DummyExporter();
        MetricReader reader = PeriodicMetricReader.create(metricExporter);
        SdkMeterProvider provider = setupProviderFromExcludesConfig(reader, "");

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

    public void testWrapMetricExporterReturnsNonNullWrapper() {
        DummyExporter delegate = new DummyExporter();
        MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));
        assertNotNull(wrapped);
        assertNotSame(delegate, wrapped);
    }

    public void testWrapMetricExporterPassesMetricsThroughWhenMetadataEmpty() {
        DummyExporter delegate = new DummyExporter();
        com.newrelic.agent.bridge.Agent mockAgent = mock(com.newrelic.agent.bridge.Agent.class);
        when(mockAgent.getServiceMetadata()).thenReturn(Collections.<String, String>emptyMap());

        MetricData md = mock(MetricData.class);

        try (MockedStatic<AgentBridge> mockBridge = Mockito.mockStatic(AgentBridge.class);
             MockedStatic<OtlpAuditLogger> ignored = Mockito.mockStatic(OtlpAuditLogger.class)) {
            mockBridge.when(AgentBridge::getAgent).thenReturn(mockAgent);
            MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));
            wrapped.export(Collections.singletonList(md));
        }

        Collection<MetricData> received = delegate.getLatestMetricData();
        assertEquals(1, received.size());
        // With empty metadata, the wrapper must forward the original MetricData unchanged (no Resource overlay).
        assertSame(md, received.iterator().next());
    }

    public void testWrapMetricExporterInjectsServiceMetadataIntoResource() {
        DummyExporter delegate = new DummyExporter();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("entity.guid", "guid-123");
        metadata.put("nr.tag.region", "us-east-1");

        com.newrelic.agent.bridge.Agent mockAgent = mock(com.newrelic.agent.bridge.Agent.class);
        when(mockAgent.getServiceMetadata()).thenReturn(metadata);

        try (MockedStatic<AgentBridge> mockBridge = Mockito.mockStatic(AgentBridge.class);
             MockedStatic<OtlpAuditLogger> ignored = Mockito.mockStatic(OtlpAuditLogger.class)) {
            mockBridge.when(AgentBridge::getAgent).thenReturn(mockAgent);
            MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));
            MetricReader reader = PeriodicMetricReader.create(wrapped);
            SdkMeterProvider provider = SdkMeterProvider.builder().registerMetricReader(reader).build();
            provider.get("test-scope").counterBuilder("my-metric").build().add(1);
            reader.forceFlush();
        }

        Collection<MetricData> exported = delegate.getLatestMetricData();
        assertEquals(1, exported.size());
        Resource resource = exported.iterator().next().getResource();
        assertEquals("guid-123", resource.getAttribute(AttributeKey.stringKey("entity.guid")));
        assertEquals("us-east-1", resource.getAttribute(AttributeKey.stringKey("nr.tag.region")));
    }

    public void testWrapMetricExporterRefreshesOverlayWhenMetadataChanges() {
        DummyExporter delegate = new DummyExporter();
        Map<String, String> first = Collections.singletonMap("entity.guid", "guid-1");
        Map<String, String> second = Collections.singletonMap("entity.guid", "guid-2");

        com.newrelic.agent.bridge.Agent mockAgent = mock(com.newrelic.agent.bridge.Agent.class);
        when(mockAgent.getServiceMetadata()).thenReturn(first, second);

        MetricData md1 = mock(MetricData.class);
        when(md1.getResource()).thenReturn(Resource.empty());
        MetricData md2 = mock(MetricData.class);
        when(md2.getResource()).thenReturn(Resource.empty());

        try (MockedStatic<AgentBridge> mockBridge = Mockito.mockStatic(AgentBridge.class);
             MockedStatic<OtlpAuditLogger> ignored = Mockito.mockStatic(OtlpAuditLogger.class)) {
            mockBridge.when(AgentBridge::getAgent).thenReturn(mockAgent);
            MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));

            wrapped.export(Collections.singletonList(md1));
            MetricData firstOut = delegate.getLatestMetricData().iterator().next();
            assertEquals("guid-1", firstOut.getResource().getAttribute(AttributeKey.stringKey("entity.guid")));

            wrapped.export(Collections.singletonList(md2));
            MetricData secondOut = delegate.getLatestMetricData().iterator().next();
            assertEquals("guid-2", secondOut.getResource().getAttribute(AttributeKey.stringKey("entity.guid")));
        }
    }

    public void testAuditModeOnCallsRecordDataUsageMetricsWhenExportCompletes() {
        DummyExporter delegate = new DummyExporter();
        com.newrelic.agent.bridge.Agent mockBridgeAgent = mock(com.newrelic.agent.bridge.Agent.class);
        when(mockBridgeAgent.getServiceMetadata()).thenReturn(Collections.<String, String>emptyMap());
        Agent mockApiAgent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        try (MockedStatic<AgentBridge> mockBridge = Mockito.mockStatic(AgentBridge.class);
             MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class);
             MockedStatic<OtlpAuditLogger> mockAuditLogger = Mockito.mockStatic(OtlpAuditLogger.class)) {
            mockBridge.when(AgentBridge::getAgent).thenReturn(mockBridgeAgent);
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockApiAgent);
            mockAuditLogger.when(OtlpAuditLogger::isAuditModeEnabled).thenReturn(true);

            MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));
            wrapped.export(Collections.<MetricData>emptyList());

            mockAuditLogger.verify(() -> OtlpAuditLogger.recordDataUsageMetrics(anyLong(), eq(0L)));
        }
    }

    public void testRecordDataUsageMetricsCalledWhenAuditModeOff() {
        DummyExporter delegate = new DummyExporter();
        com.newrelic.agent.bridge.Agent mockBridgeAgent = mock(com.newrelic.agent.bridge.Agent.class);
        when(mockBridgeAgent.getServiceMetadata()).thenReturn(Collections.<String, String>emptyMap());

        try (MockedStatic<AgentBridge> mockBridge = Mockito.mockStatic(AgentBridge.class);
             MockedStatic<OtlpAuditLogger> mockAuditLogger = Mockito.mockStatic(OtlpAuditLogger.class)) {
            mockBridge.when(AgentBridge::getAgent).thenReturn(mockBridgeAgent);
            mockAuditLogger.when(OtlpAuditLogger::isAuditModeEnabled).thenReturn(false);

            MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));
            wrapped.export(Collections.<MetricData>emptyList());

            // Data usage metrics are recorded on every export regardless of audit mode.
            mockAuditLogger.verify(() -> OtlpAuditLogger.recordDataUsageMetrics(anyLong(), eq(0L)));
        }
    }

    public void testExportSuccessRecordsSuccessMetric() {
        DummyExporter delegate = new DummyExporter();
        com.newrelic.agent.bridge.Agent mockBridgeAgent = mock(com.newrelic.agent.bridge.Agent.class);
        when(mockBridgeAgent.getServiceMetadata()).thenReturn(Collections.<String, String>emptyMap());
        // wrapMetricExporter() calls applyProxy() before returning, which reads PROXY_HOST via
        // NewRelic.getAgent() -- stub it here so that call does not NPE.
        Agent mockApiAgent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockApiAgent.getConfig().getValue(PROXY_HOST)).thenReturn(null);

        try (MockedStatic<AgentBridge> mockBridge = Mockito.mockStatic(AgentBridge.class);
             MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class);
             MockedStatic<OtlpAuditLogger> mockAuditLogger = Mockito.mockStatic(OtlpAuditLogger.class)) {
            mockBridge.when(AgentBridge::getAgent).thenReturn(mockBridgeAgent);
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockApiAgent);
            mockAuditLogger.when(OtlpAuditLogger::isAuditModeEnabled).thenReturn(false);

            MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));
            wrapped.export(Collections.<MetricData>emptyList());

            mockNewRelic.verify(() -> NewRelic.incrementCounter("Supportability/Metrics/Java/OpenTelemetryBridge/export/success"));
            mockNewRelic.verify(() -> NewRelic.incrementCounter("Supportability/Metrics/Java/OpenTelemetryBridge/export/failure"), Mockito.never());
        }
    }

    public void testExportFailureRecordsFailureMetric() {
        DummyExporter delegate = new DummyExporter();
        delegate.setExportResult(CompletableResultCode.ofFailure());
        com.newrelic.agent.bridge.Agent mockBridgeAgent = mock(com.newrelic.agent.bridge.Agent.class);
        when(mockBridgeAgent.getServiceMetadata()).thenReturn(Collections.<String, String>emptyMap());
        // wrapMetricExporter() calls applyProxy() before returning, which reads PROXY_HOST via
        // NewRelic.getAgent() -- stub it here so that call does not NPE.
        Agent mockApiAgent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockApiAgent.getConfig().getValue(PROXY_HOST)).thenReturn(null);

        try (MockedStatic<AgentBridge> mockBridge = Mockito.mockStatic(AgentBridge.class);
             MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class);
             MockedStatic<OtlpAuditLogger> mockAuditLogger = Mockito.mockStatic(OtlpAuditLogger.class)) {
            mockBridge.when(AgentBridge::getAgent).thenReturn(mockBridgeAgent);
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockApiAgent);
            mockAuditLogger.when(OtlpAuditLogger::isAuditModeEnabled).thenReturn(false);

            MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));
            wrapped.export(Collections.<MetricData>emptyList());

            // export/failure covers any dropped batch, including this non-retryable case, not just
            // retry exhaustion.
            mockNewRelic.verify(() -> NewRelic.incrementCounter("Supportability/Metrics/Java/OpenTelemetryBridge/export/failure"));
            mockNewRelic.verify(() -> NewRelic.incrementCounter("Supportability/Metrics/Java/OpenTelemetryBridge/export/success"), Mockito.never());
        }
    }

    public void testWrapMetricExporterDelegatesPassThroughMethods() {
        MetricExporter delegate = mock(MetricExporter.class);
        when(delegate.getDefaultAggregation(InstrumentType.COUNTER)).thenReturn(Aggregation.sum());
        when(delegate.getAggregationTemporality(InstrumentType.COUNTER)).thenReturn(DELTA);
        when(delegate.getMemoryMode()).thenReturn(MemoryMode.REUSABLE_DATA);
        when(delegate.flush()).thenReturn(CompletableResultCode.ofSuccess());
        when(delegate.shutdown()).thenReturn(CompletableResultCode.ofSuccess());

        MetricExporter wrapped = OpenTelemetrySDKCustomizer.wrapMetricExporter(delegate, mock(ConfigProperties.class));

        assertSame(Aggregation.sum(), wrapped.getDefaultAggregation(InstrumentType.COUNTER));
        assertEquals(DELTA, wrapped.getAggregationTemporality(InstrumentType.COUNTER));
        assertEquals(MemoryMode.REUSABLE_DATA, wrapped.getMemoryMode());
        assertTrue(wrapped.flush().isSuccess());
        assertTrue(wrapped.shutdown().isSuccess());

        wrapped.close();
        verify(delegate).close();
    }

    public void testBuildProxyOptions() throws Exception {
        ProxyOptions options = OpenTelemetrySDKCustomizer.buildProxyOptions("myproxy", 3128);
        assertNotNull(options);

        List<Proxy> proxies = options.getProxySelector().select(new URI("https://collector.newrelic.com:443"));
        assertEquals(1, proxies.size());
        Proxy proxy = proxies.get(0);
        assertEquals(Proxy.Type.HTTP, proxy.type());
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        assertEquals("myproxy", address.getHostString());
        assertEquals(3128, address.getPort());
    }

    public void testApplyProxyReturnsSameExporterWhenNoProxyConfigured() {
        DummyExporter delegate = new DummyExporter();
        Agent mockAgent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockAgent.getConfig().getValue(PROXY_HOST)).thenReturn(null);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            MetricExporter result = OpenTelemetrySDKCustomizer.applyProxy(delegate);
            assertSame(delegate, result);
        }
    }

    public void testApplyProxyReturnsSameExporterForNonOtlpExporter() {
        DummyExporter delegate = new DummyExporter();
        Agent mockAgent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockAgent.getConfig().getValue(PROXY_HOST)).thenReturn("myproxy");
        when(mockAgent.getConfig().getValue(PROXY_PORT, DEFAULT_PROXY_PORT)).thenReturn(3128);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            MetricExporter result = OpenTelemetrySDKCustomizer.applyProxy(delegate);
            // A non-OtlpHttpMetricExporter cannot be rebuilt with proxy options, so it is returned unchanged.
            assertSame(delegate, result);
        }
    }

    public void testApplyProxyRebuildsOtlpExporterWithProxy() {
        OtlpHttpMetricExporter original =
                OtlpHttpMetricExporter.builder().setEndpoint("http://example.test:4318/v1/metrics").build();
        Agent mockAgent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockAgent.getConfig().getValue(PROXY_HOST)).thenReturn("myproxy");
        when(mockAgent.getConfig().getValue(PROXY_PORT, DEFAULT_PROXY_PORT)).thenReturn(3128);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            MetricExporter result = OpenTelemetrySDKCustomizer.applyProxy(original);
            // The exporter is rebuilt (new instance) while preserving the configured endpoint.
            assertNotSame(original, result);
            assertTrue(result instanceof OtlpHttpMetricExporter);
            assertTrue(result.toString().contains("example.test"));
        }
    }

    private List<String> metricNames(Collection<MetricData> collectedMetrics) {
        List<String> metricNames = new ArrayList<>();
        for (MetricData metricData : collectedMetrics) {
            metricNames.add(metricData.getName());
        }
        return metricNames;
    }

    private SdkMeterProvider setupProviderFromExcludesConfig(MetricReader reader, String excludedMeters) {
        Agent agent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Logger logger = mock(Logger.class);
        when(agent.getLogger()).thenReturn(logger);
        Mockito.when(agent.getConfig().getValue(OPENTELEMETRY_METRICS_EXCLUDE)).thenReturn(excludedMeters);
        SdkMeterProviderBuilder customizedBuilder;
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(agent);
            customizedBuilder = OpenTelemetrySDKCustomizer.applyMeterExcludes(
                    SdkMeterProvider.builder().registerMetricReader(reader), agent
            );
        }
        return customizedBuilder.build();
    }

    // A dummy exporter exposes the last round of collected metrics on its result field.
    static class DummyExporter implements MetricExporter {

        //expose the most recently exported metrics
        private Collection<MetricData> lastestMetricData = null;

        // the result returned by export() - defaults to success, tests may override it to
        // exercise the failure path
        private CompletableResultCode exportResult = CompletableResultCode.ofSuccess();

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
            return exportResult;
        }

        void setExportResult(CompletableResultCode exportResult) {
            this.exportResult = exportResult;
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }

        public Collection<MetricData> getLatestMetricData() {
            return lastestMetricData;
        }
    }
}