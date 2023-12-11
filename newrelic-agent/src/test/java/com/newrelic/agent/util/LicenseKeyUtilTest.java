/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;

public class LicenseKeyUtilTest extends TestCase {

    public void testObfuscateLicenseKey() {
        // Given
        String originalRequestUrl = "https://staging-collector.newrelic.com:443/agent_listener/invoke_raw_method?method=connect&license_key=abcdefghijklmonpqrstuvwxyz1234567890&marshal_format=json&protocol_version=17";

        String originalJsonPayload = "[{\"license_key\":\"abcdefghijklmonpqrstuvwxyz1234567890\"}]";

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("license_key", "abcdefghijklmonpqrstuvwxyz1234567890");

        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        // When
        String actualRequestUrl = LicenseKeyUtil.obfuscateLicenseKey(originalRequestUrl);
        String actualJsonPayload = LicenseKeyUtil.obfuscateLicenseKey(originalJsonPayload);

        // Then
        String expectedRequestUrl = "https://staging-collector.newrelic.com:443/agent_listener/invoke_raw_method?method=connect&license_key=obfuscated&marshal_format=json&protocol_version=17";

        String expectedJsonPayload = "[{\"license_key\":\"obfuscated\"}]";

        Assert.assertEquals(expectedRequestUrl, actualRequestUrl);
        Assert.assertEquals(expectedJsonPayload, actualJsonPayload);
    }

    public void testObfuscateLicenseKeyWithMultipleLicenseKeyEntries() {
        // Given
        String originalJsonPayload = "[" +
                "{\"license_key\":\"abcdefghijklmonpqrstuvwxyz1234567890\"}, {\"license_key\":\"abcdefghijklmonpqrstuvwxyz1234567890\"}, {\"license_key\":\"abcdefghijklmonpqrstuvwxyz1234567890\"}, {\"license_key\":\"abcdefghijklmonpqrstuvwxyz1234567890\"}]";

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("license_key", "abcdefghijklmonpqrstuvwxyz1234567890");

        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        // When
        String actualJsonPayload = LicenseKeyUtil.obfuscateLicenseKey(originalJsonPayload);

        // Then
        String expectedJsonPayload = "[" +
                "{\"license_key\":\"obfuscated\"}, {\"license_key\":\"obfuscated\"}, {\"license_key\":\"obfuscated\"}, {\"license_key\":\"obfuscated\"}]";

        Assert.assertEquals(expectedJsonPayload, actualJsonPayload);
    }

    public void testObfuscateLicenseKeyWithNullLicenseKey() {
        // Given
        String originalRequestUrl = "https://staging-collector.newrelic.com:443/agent_listener/invoke_raw_method?method=connect&license_key=abcdefghijklmonpqrstuvwxyz1234567890&marshal_format=json&protocol_version=17";

        String originalJsonPayload = "[{\"license_key\":\"abcdefghijklmonpqrstuvwxyz1234567890\"}]";

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("license_key", null);

        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        // When
        String actualRequestUrl = LicenseKeyUtil.obfuscateLicenseKey(originalRequestUrl);
        String actualJsonPayload = LicenseKeyUtil.obfuscateLicenseKey(originalJsonPayload);

        // Then
        Assert.assertEquals(originalRequestUrl, actualRequestUrl);
        Assert.assertEquals(originalJsonPayload, actualJsonPayload);
    }

    public void testObfuscateLicenseKeyWithNullOrEmptyString() {
        // Given
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("license_key", "abcdefghijklmonpqrstuvwxyz1234567890");

        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        // When
        String actualEmptyString = LicenseKeyUtil.obfuscateLicenseKey("");
        String actualNullString = LicenseKeyUtil.obfuscateLicenseKey(null);

        // Then
        Assert.assertEquals("", actualEmptyString);
        Assert.assertNull(actualNullString);
    }
}
