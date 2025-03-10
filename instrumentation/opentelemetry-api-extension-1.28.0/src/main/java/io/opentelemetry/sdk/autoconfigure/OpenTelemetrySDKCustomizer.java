///*
// *
// *  * Copyright 2024 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package io.opentelemetry.sdk.autoconfigure;
//
//import com.newrelic.agent.bridge.AgentBridge;
//import com.newrelic.api.agent.Agent;
//import com.newrelic.api.agent.Logger;
//import com.newrelic.api.agent.NewRelic;
//import io.opentelemetry.api.common.AttributeKey;
//import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
//import io.opentelemetry.sdk.resources.Resource;
//import io.opentelemetry.sdk.resources.ResourceBuilder;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.logging.Level;
//
///**
// * Helper class for customizing OpenTelemetrySDK properties
// * and resources for compatability with New Relic.
// */
//final class OpenTelemetrySDKCustomizer {
//    static final AttributeKey<String> SERVICE_INSTANCE_ID_ATTRIBUTE_KEY = AttributeKey.stringKey("service.instance.id");
//
//    static Map<String, String> applyProperties(ConfigProperties configProperties) {
//        return applyProperties(configProperties, NewRelic.getAgent());
//    }
//
//    /**
//     * Configure OpenTelemetry exporters to send data to the New Relic backend.
//     */
//    static Map<String, String> applyProperties(ConfigProperties configProperties, Agent agent) {
//        final String existingEndpoint = configProperties.getString("otel.exporter.otlp.endpoint");
//        if (existingEndpoint == null) {
//            agent.getLogger().log(Level.INFO, "Auto-initializing OpenTelemetry SDK");
//            final String host = agent.getConfig().getValue("host");
//            final String endpoint = "https://" + host + ":443";
//            final String licenseKey = agent.getConfig().getValue("license_key");
//            final Map<String, String> properties = new HashMap<>();
//            properties.put("otel.exporter.otlp.headers", "api-key=" + licenseKey);
//            properties.put("otel.exporter.otlp.endpoint", endpoint);
//            properties.put("otel.exporter.otlp.protocol", "http/protobuf");
//            properties.put("otel.span.attribute.value.length.limit", "4095");
//            properties.put("otel.exporter.otlp.compression", "gzip");
//            properties.put("otel.exporter.otlp.metrics.temporality.preference", "DELTA");
//            properties.put("otel.exporter.otlp.metrics.default.histogram.aggregation", "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM");
//            properties.put("otel.experimental.exporter.otlp.retry.enabled", "true");
//            properties.put("otel.experimental.resource.disabled.keys", "process.command_line");
//
//            final Object appName = agent.getConfig().getValue("app_name");
//            properties.put("otel.service.name", appName.toString());
//
//            return properties;
//        } else {
//            agent.getLogger().log(Level.WARNING,
//                    "The OpenTelemetry exporter endpoint is set to {0}, the agent will not autoconfigure the SDK",
//                    existingEndpoint);
//        }
//        return Collections.emptyMap();
//    }
//
//    static Resource applyResources(Resource resource, ConfigProperties configProperties) {
//        return applyResources(resource, AgentBridge.getAgent(), NewRelic.getAgent().getLogger());
//    }
//
//    /**
//     * Add the monitored service's entity.guid to resources.
//     */
//    static Resource applyResources(Resource resource, com.newrelic.agent.bridge.Agent agent, Logger logger) {
//        logger.log(Level.FINE, "Appending OpenTelemetry resources");
//        final ResourceBuilder builder = new ResourceBuilder().putAll(resource);
//        final String instanceId = resource.getAttribute(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY);
//        if (instanceId == null) {
//            builder.put(SERVICE_INSTANCE_ID_ATTRIBUTE_KEY, UUID.randomUUID().toString());
//        }
//
//        final String entityGuid = agent.getEntityGuid(true);
//        if (entityGuid != null) {
//            builder.put("entity.guid", entityGuid);
//        }
//        return builder.build();
//    }
//}
