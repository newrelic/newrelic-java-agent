/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.getOpenTelemetryProxyHost;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.getOpenTelemetryProxyPort;

import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.getOpenTelemetryMetricsExcludes;

/**
 * Helper class for customizing OpenTelemetrySDK properties
 * and resources for compatability with New Relic.
 */
final class OpenTelemetrySDKCustomizer {
    static final AttributeKey<String> SERVICE_INSTANCE_ID_ATTRIBUTE_KEY = AttributeKey.stringKey("service.instance.id");

    private static final String DEFAULT_COLLECTOR_HOST = "collector.newrelic.com";

    static Map<String, String> applyProperties(ConfigProperties configProperties) {
        return applyProperties(configProperties, NewRelic.getAgent());
    }

    /**
     * Configure OpenTelemetry exporters to send data to the New Relic backend.
     */
    static Map<String, String> applyProperties(ConfigProperties configProperties, Agent agent) {
        final String existingEndpoint = configProperties.getString("otel.exporter.otlp.endpoint");
        if (existingEndpoint == null) {
            agent.getLogger().log(Level.INFO, "Auto-initializing OpenTelemetry SDK");
            String host = agent.getConfig().getValue("host");
            if (host == null) {
                host = DEFAULT_COLLECTOR_HOST;
                agent.getLogger().log(Level.WARNING,
                        "No host was configured for the OpenTelemetry metrics exporter endpoint. The exporter will use the default host for the New Relic US Production region: {0}",
                        DEFAULT_COLLECTOR_HOST);
            }
            final String endpoint = "https://" + host + ":443";
            final String licenseKey = agent.getConfig().getValue("license_key");
            final Map<String, String> properties = new HashMap<>();
            properties.put("otel.exporter.otlp.headers", "api-key=" + licenseKey);
            properties.put("otel.exporter.otlp.endpoint", endpoint);
            properties.put("otel.metrics.exporter", "otlp"); // enable otlp metrics exporter
            properties.put("otel.traces.exporter", "none"); // disable default traces exporter
            properties.put("otel.logs.exporter", "none"); // disable default logs exporter
            // otel.metric.export.interval should be set before otel.exporter.otlp.metrics.timeout for validation purposes
            properties.put("otel.metric.export.interval",
                    String.valueOf(OpenTelemetryConfig.getOpenTelemetryMetricsExportInterval())); // metric reporting interval in milliseconds
            properties.put("otel.exporter.otlp.metrics.timeout",
                    String.valueOf(OpenTelemetryConfig.getOpenTelemetryMetricsExportTimeout())); // metric reporting timeout in milliseconds
            properties.put("otel.exporter.otlp.protocol", "http/protobuf");
            properties.put("otel.span.attribute.value.length.limit", "4095");
            properties.put("otel.exporter.otlp.compression", "gzip");
            properties.put("otel.exporter.otlp.metrics.temporality.preference", "DELTA");
            properties.put("otel.exporter.otlp.metrics.default.histogram.aggregation", "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM");
            properties.put("otel.experimental.exporter.otlp.retry.enabled", "true");
            properties.put("otel.experimental.resource.disabled.keys", "process.command_line");

            final Object appName = agent.getConfig().getValue("app_name");
            properties.put("otel.service.name", appName.toString());

            return properties;
        } else {
            agent.getLogger().log(Level.WARNING,
                    "The OpenTelemetry exporter endpoint is set to {0}, the agent will not autoconfigure the SDK",
                    existingEndpoint);
        }
        return Collections.emptyMap();
    }

    static Resource applyResources(Resource resource, ConfigProperties configProperties) {
        return applyResources(resource, AgentBridge.getAgent(), NewRelic.getAgent().getLogger());
    }

    /**
     * Add the monitored service's entity.guid to resources.
     */
    static Resource applyResources(Resource resource, com.newrelic.agent.bridge.Agent agent, Logger logger) {
        logger.log(Level.FINE, "Appending OpenTelemetry resources");
        final ResourceBuilder builder = new ResourceBuilder().putAll(resource);
        final String instanceId = resource.getAttribute(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY);
        if (instanceId == null) {
            builder.put(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY, UUID.randomUUID().toString());
        }

        // This blocks for up to 1 minute to ensure that an entity guid is returned from a connect response
        final String entityGuid = agent.getEntityGuid(true);
        if (entityGuid != null) {
            builder.put("entity.guid", entityGuid);
        }
        return builder.build();
    }

    /**
     * Wrap the metric exporter to inject updated service metadata into exported MetricData resources.
     * If a proxy is configured, the exporter is first rebuilt to route OTLP metric exports through it.
     */
    static MetricExporter wrapMetricExporter(MetricExporter exporter, ConfigProperties configProperties) {
        return new NRMetricExporterWrapper(applyProxy(exporter));
    }

    /**
     * Route OTLP dimensional metric exports through the agent's configured proxy (reusing the top-level
     * {@code proxy_host}/{@code proxy_port} settings). The OpenTelemetry SDK cannot express proxy
     * authentication or a proxy scheme, so {@code proxy_user}/{@code proxy_password}/{@code proxy_scheme}
     * are not applied here.
     *
     * @param exporter the metric exporter built by the OpenTelemetry SDK
     * @return the exporter rebuilt with proxy options when a proxy is configured, otherwise the original exporter
     */
    static MetricExporter applyProxy(MetricExporter exporter) {
        final String proxyHost = getOpenTelemetryProxyHost();
        if (proxyHost == null || proxyHost.trim().isEmpty()) {
            return exporter;
        }

        final int proxyPort = getOpenTelemetryProxyPort();
        if (exporter instanceof OtlpHttpMetricExporter) {
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.INFO, "Routing OpenTelemetry dimensional metric exports through proxy {0}:{1}", proxyHost, proxyPort);
            return ((OtlpHttpMetricExporter) exporter).toBuilder()
                    .setProxyOptions(buildProxyOptions(proxyHost, proxyPort))
                    .build();
        }

        NewRelic.getAgent()
                .getLogger()
                .log(Level.WARNING,
                        "A proxy is configured but the OpenTelemetry metric exporter is not an OtlpHttpMetricExporter (found {0}). "
                                + "Dimensional metric exports will not be routed through the proxy.",
                        exporter.getClass().getName());
        return exporter;
    }

    /**
     * Build {@link ProxyOptions} for an unauthenticated HTTP proxy at the given host and port. The
     * address is left unresolved so DNS resolution is deferred to export time.
     */
    static ProxyOptions buildProxyOptions(String host, int port) {
        return ProxyOptions.create(InetSocketAddress.createUnresolved(host, port));
    }

    /**
     * Read list of excluded meters, and customize the meter provider to drop any with matching names.
     */
    static SdkMeterProviderBuilder applyMeterExcludes(SdkMeterProviderBuilder sdkMeterProviderBuilder, ConfigProperties configProperties) {
        return applyMeterExcludes(sdkMeterProviderBuilder, NewRelic.getAgent());
    }

    static SdkMeterProviderBuilder applyMeterExcludes(SdkMeterProviderBuilder sdkMeterProviderBuilder, Agent agent) {
        final List<String> excludedMeters = getOpenTelemetryMetricsExcludes();
        agent.getLogger().log(Level.FINE, "Suppressing excluded OpenTelemetry meters: {0}", excludedMeters);
        for (String meterName : excludedMeters) {
            sdkMeterProviderBuilder.registerView(
                    InstrumentSelector.builder().setMeterName(meterName).build(),
                    View.builder().setAggregation(Aggregation.drop()).build()
            );
        }
        return sdkMeterProviderBuilder;
    }
}
