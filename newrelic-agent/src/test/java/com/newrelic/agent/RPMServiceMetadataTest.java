/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RPMServiceMetadataTest {

    private RPMService rpmService;

    @Before
    public void setup() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "MyApp");
        map.put("host", "no-collector.example.com");
        map.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");

        AgentConfig config = AgentConfigImpl.createAgentConfig(map);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, map);

        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        rpmService = new RPMService(singletonList("MyApp"), null, null,
                Collections.<AgentConnectionEstablishedListener>emptyList());
    }

    @After
    public void teardown() {
        // Avoid rpmService.shutdown() — it depends on services not initialized in this minimal setup.
        rpmService = null;
    }

    @Test
    public void getServiceMetadata_defaults_to_empty_map() {
        Map<String, String> metadata = rpmService.getServiceMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.isEmpty());
    }

    @Test
    public void buildServiceMetadata_includes_otlp_resource_attributes() throws Exception {
        Map<String, String> otlp = new HashMap<>();
        otlp.put("tags.region", "us-east-1");
        otlp.put("host.displayName", "host-42");
        otlp.put("entity.guid", "guid-123");
        Map<String, Object> connect = new HashMap<>();
        connect.put(RPMService.OTLP_RESOURCE_ATTRIBUTES, otlp);

        Map<String, String> result = invokeBuildServiceMetadata(rpmService, connect);
        assertEquals(3, result.size());
        assertEquals("us-east-1", result.get("tags.region"));
        assertEquals("host-42", result.get("host.displayName"));
        assertEquals("guid-123", result.get("entity.guid"));
    }

    @Test
    public void buildServiceMetadata_returns_empty_when_entity_guid_blank_and_no_otlp() throws Exception {
        Map<String, String> result = invokeBuildServiceMetadata(rpmService, Collections.<String, Object>emptyMap());
        assertTrue(result.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void buildServiceMetadata_returns_unmodifiable_map() throws Exception {
        Map<String, String> result = invokeBuildServiceMetadata(rpmService, Collections.<String, Object>emptyMap());
        result.put("attempt", "modify");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> invokeBuildServiceMetadata(RPMService svc, Map<String, Object> connectData) throws Exception {
        Method m = RPMService.class.getDeclaredMethod("buildServiceMetadata", Map.class);
        m.setAccessible(true);
        return (Map<String, String>) m.invoke(svc, connectData);
    }
}