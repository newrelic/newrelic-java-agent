/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.config.UrlPathObfuscationConfigImpl;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HttpParameters;
import org.json.simple.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class TransactionSegmentTest {

    @Test
    public void testTransactionSegmentFilter() throws Exception {
        // Exclude red, yellow, and http.url attributes
        Map<String, Object> transactionSegmentsConfig = new HashMap<>();
        Map<String, Object> attributesMap = new HashMap<>();
        transactionSegmentsConfig.put("attributes", attributesMap);
        attributesMap.put("enabled", true);
        attributesMap.put("exclude", "red,yellow,http.url");
        MockServiceManager manager = setupServiceManager(transactionSegmentsConfig);

        Map<String, Object> tracerAttributes = new HashMap<>();
        tracerAttributes.put("red", "red");
        tracerAttributes.put("yellow", "yellow");
        tracerAttributes.put("green", "green");

        TransactionSegment segment = createSegment(manager, tracerAttributes);
        JSONArray array = (JSONArray) AgentHelper.serializeJSON(segment);

        Map<String, Object> attributes = (Map<String, Object>) array.get(3);
        Assert.assertEquals("green", attributes.get("green"));
        // Red and yellow should be filtered out
        Assert.assertNull(attributes.get("red"));
        Assert.assertNull(attributes.get("yellow"));
        Assert.assertNull(attributes.get("http.url"));
    }

    @Test
    public void testLegacyTransactionSegmentUriIsObfuscated() {
        MockServiceManager manager = setupServiceManager(new HashMap<>());
        configureObfuscation("(/accounts/)\\d+", "$1REDACTED");

        // getExternalParameters() == null: this tracer never called reportAsExternal(), so its uri was
        // never run through ExternalsUtil.sanitizeURI()
        TransactionSegment segment = createSegmentWithUri(manager, "http://host:1234/accounts/123456", null);

        Assert.assertEquals("http://host:1234/accounts/REDACTED", segment.getUri());
    }

    @Test
    public void testLegacyTransactionSegmentUriThatCannotBeParsed_obfuscatesWholeString() {
        MockServiceManager manager = setupServiceManager(new HashMap<>());
        configureObfuscation("(/accounts/)\\d+", "$1REDACTED");

        // the space makes this an invalid URI, so new URI(...) throws and the whole string is obfuscated as-is
        TransactionSegment segment = createSegmentWithUri(manager, "host:8080 /accounts/123456", null);

        Assert.assertEquals("host:8080 /accounts/REDACTED", segment.getUri());
    }

    @Test
    public void testExternalParametersUriIsNotDoubleObfuscated() {
        MockServiceManager manager = setupServiceManager(new HashMap<>());
        configureObfuscation("(/accounts/)\\d+", "$1REDACTED");

        ExternalParameters externalParameters = HttpParameters.library("lib")
                .uri(URI.create("http://host/accounts/123456"))
                .procedure("GET")
                .noInboundHeaders()
                .build();

        // already obfuscated by ExternalsUtil.sanitizeURI() by the time reportAsExternal() ran
        TransactionSegment segment = createSegmentWithUri(manager, "http://host/accounts/REDACTED", externalParameters);

        Assert.assertEquals("http://host/accounts/REDACTED", segment.getUri());
    }

    private TransactionSegment createSegmentWithUri(MockServiceManager manager, String uri, ExternalParameters externalParameters) {
        TransactionTracerConfig ttConfig = manager.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(new ClassMethodSignature("class", "method", "methodDesc"));
        Mockito.when(tracer.getTransactionSegmentName()).thenReturn("segmentName");
        Mockito.when(tracer.getAgentAttributes()).thenReturn(new HashMap<>());
        Mockito.when(tracer.getTransactionSegmentUri()).thenReturn(uri);
        Mockito.when(tracer.getExternalParameters()).thenReturn(externalParameters);

        SqlObfuscator obfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        String appName = manager.getConfigService().getDefaultAgentConfig().getApplicationName();
        return new TransactionSegment(ttConfig, appName, obfuscator, 0, tracer, null);
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

    private TransactionSegment createSegment(MockServiceManager manager, Map<String, Object> tracerAttributes) {
        TransactionTracerConfig ttConfig = manager.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(new ClassMethodSignature("class", "method", "methodDesc"));
        Mockito.when(tracer.getTransactionSegmentName()).thenReturn("segmentName");
        Mockito.when(tracer.getAgentAttributes()).thenReturn(tracerAttributes);
        Mockito.when(tracer.getTransactionSegmentUri()).thenReturn("http://host:1234/path/to/chocolate");

        SqlObfuscator obfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        String appName = manager.getConfigService().getDefaultAgentConfig().getApplicationName();
        return new TransactionSegment(ttConfig, appName, obfuscator, 0, tracer, null);
    }

    private MockServiceManager setupServiceManager(Map<String, Object> transactionSegmentsConfig) {
        final Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, "name");
        config.put(AgentConfigImpl.TRANSACTION_SEGMENTS, transactionSegmentsConfig);
        MockServiceManager manager = new MockServiceManager();
        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(config), config);
        manager.setConfigService(configService);
        manager.setAttributesService(new AttributesService());
        ServiceFactory.setServiceManager(manager);
        return manager;
    }

}