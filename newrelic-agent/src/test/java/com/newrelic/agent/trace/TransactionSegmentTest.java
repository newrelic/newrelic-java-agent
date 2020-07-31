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
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import org.json.simple.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

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