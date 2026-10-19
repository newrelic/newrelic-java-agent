/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.util;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.*;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UrlPathObfuscatorServiceTest {

    private MockServiceManager serviceManager;
    private UrlPathObfuscatorService obfuscator;

    @Before
    public void setup() {
        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        Map<String, Object> configMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        obfuscator = new UrlPathObfuscatorService();
    }

    @Test
    public void nullPath_returnsNull() {
        obfuscator.configChanged("test_app",  AgentConfigImpl.createAgentConfig(
                createConfigMap(true, "ABC", "XYZ", true)));

        assertNull(obfuscator.obfuscatePath(null));
    }

    @Test
    public void emptyPath_returnsEmptyString() {
        obfuscator.configChanged("test_app",  AgentConfigImpl.createAgentConfig(
                createConfigMap(true, "ABC", "XYZ", true)));

        assertEquals("", obfuscator.obfuscatePath(""));
    }

    @Test
    public void obfuscationDisabled_returnsUnchangedPath() {
        obfuscator.configChanged("test_app",  AgentConfigImpl.createAgentConfig(
                createConfigMap(false, "ABC", "XYZ", true)));

        assertEquals("1234", obfuscator.obfuscatePath("1234"));
    }

    @Test
    public void obfuscationEnabled_withValidRegExAndEmptyReplacement_returnsEmptyString() {
        obfuscator.configChanged("test_app",  AgentConfigImpl.createAgentConfig(
                createConfigMap(true, "ABC", "", true)));

        assertEquals("", obfuscator.obfuscatePath("ABC"));
    }

    @Test
    public void obfuscationEnabled_withValidRegExAndReplacement_returnsModifiedString() {
        obfuscator.configChanged("test_app",  AgentConfigImpl.createAgentConfig(
                createConfigMap(true, "ABC", "XYZ", true)));

        assertEquals("XYZ", obfuscator.obfuscatePath("ABC"));
    }

    @Test
    public void obfuscationEnabled_withComplexRegEx_obfuscatesCorrectly() {
        String pattern = "(/accounts/)(\\d{4,10}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/transactions)?";
        String replacement = "$1{id}$3";

        obfuscator.configChanged("test_app", AgentConfigImpl.createAgentConfig(
                createConfigMap(true, pattern, replacement, false)));

        assertEquals("/accounts/{id}/transactions", obfuscator.obfuscatePath("/accounts/123456789/transactions"));
        assertEquals("/accounts/{id}", obfuscator.obfuscatePath("/accounts/9f8e7d6c-5a4b-3c2d-1e0f-abcdef123456"));
    }

    private Map<String, Object> createConfigMap(boolean isEnabled, String regEx, String replacement, boolean ignoreCase) {
        Map<String, Object> regexMap = new HashMap<>();
        regexMap.put("pattern", regEx);
        regexMap.put("replacement", replacement);
        regexMap.put("ignore_case", ignoreCase);

        Map<String, Object> urlObfuscationMap = new HashMap<>();
        urlObfuscationMap.put(UrlPathObfuscationConfigImpl.ENABLED, isEnabled);
        urlObfuscationMap.put(UrlPathObfuscationConfigImpl.REGEX, regexMap);

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(AgentConfigImpl.URL_OBFUSCATION, urlObfuscationMap);
        return configMap;
    }
}
