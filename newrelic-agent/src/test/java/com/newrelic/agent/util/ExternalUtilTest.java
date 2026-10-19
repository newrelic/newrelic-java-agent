/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.util;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.*;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExternalUtilTest {
    private MockServiceManager serviceManager;

    @Before
    public void setup() {
        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        Map<String, Object> configMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);
    }

    @Test
    public void testSanitizeUri_obfuscationDisabled_pathUnchanged() throws Exception {
        URI uri = new URI("https://example.com:8080/accounts/123456");

        URI sanitized = ExternalsUtil.sanitizeURI(uri);

        assertEquals("/accounts/123456", sanitized.getPath());
        assertEquals("example.com", sanitized.getHost());
        assertEquals(8080, sanitized.getPort());
        assertEquals("https", sanitized.getScheme());
    }

    @Test
    public void testSanitizeUri_obfuscationEnabled_pathObfuscated_hostAndPortUnchanged() throws Exception {
        configureObfuscation("(/accounts/)\\d+", "$1{id}");

        URI uri = new URI("https://example.com:8080/accounts/123456");

        URI sanitized = ExternalsUtil.sanitizeURI(uri);

        assertEquals("/accounts/{id}", sanitized.getPath());
        assertEquals("example.com", sanitized.getHost());
        assertEquals(8080, sanitized.getPort());
        assertEquals("https", sanitized.getScheme());
    }

    @Test
    public void testSanitizeUri_obfuscationEnabled_noMatch_pathUnchanged() throws Exception {
        configureObfuscation("(/accounts/)\\d+", "$1{id}");

        URI uri = new URI("https://example.com/orders/789");

        URI sanitized = ExternalsUtil.sanitizeURI(uri);

        assertEquals("/orders/789", sanitized.getPath());
    }

    @Test
    public void testSanitizeUri_obfuscationEnabled_queryAndFragmentStillStripped() throws Exception {
        configureObfuscation("(/accounts/)\\d+", "$1{id}");

        URI uri = new URI("https://example.com/accounts/123456?foo=bar#frag");

        URI sanitized = ExternalsUtil.sanitizeURI(uri);

        assertEquals("/accounts/{id}", sanitized.getPath());
        assertNull(sanitized.getQuery());
        assertNull(sanitized.getFragment());
    }

    private void configureObfuscation(String pattern, String replacement) {
        Map<String, Object> regexMap = new HashMap<>();
        regexMap.put("pattern", pattern);
        regexMap.put("replacement", replacement);

        Map<String, Object> urlObfuscationMap = new HashMap<>();
        urlObfuscationMap.put(UrlPathObfuscationConfigImpl.ENABLED, true);
        urlObfuscationMap.put(UrlPathObfuscationConfigImpl.REGEX, regexMap);

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(AgentConfigImpl.URL_OBFUSCATION, urlObfuscationMap);

        ServiceFactory.getUrlPathObfuscator().configChanged("Unit Test", AgentConfigImpl.createAgentConfig(configMap));
    }
}
