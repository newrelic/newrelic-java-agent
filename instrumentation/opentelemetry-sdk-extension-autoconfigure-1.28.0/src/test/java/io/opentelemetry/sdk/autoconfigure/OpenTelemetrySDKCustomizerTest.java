/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import junit.framework.TestCase;

import java.util.Map;

import static io.opentelemetry.sdk.autoconfigure.OpenTelemetrySDKCustomizer.SERVICE_INSTANCE_ID_ATTRIBUTE_KEY;
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
}
