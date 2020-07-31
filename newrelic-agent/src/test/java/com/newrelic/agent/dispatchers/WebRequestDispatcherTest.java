/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.dispatchers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionEventsConfig;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.api.agent.Response;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebRequestDispatcherTest {
    private static final String HEADER_WITH_ALIAS = "x-header-with-alias";
    private static final String HEADER_ALIAS = "TraceID";
    private static final String HEADER_WITHOUT_ALIAS = "x-header-without-alias";
    private static final String UNUSED_HEADER = "x-unused-header";

    private TransactionStats stats;
    private static final String APP_NAME = "Unit Test";

    @Test
    public void testMultipleMixedCustomHeaderConfigs() throws Exception {
        initializeTest(false);
        Map<String, Object> userAttributes = runTransactionAndGetAttributes();

        Assert.assertTrue(userAttributes.containsKey(HEADER_ALIAS));
        Assert.assertTrue(userAttributes.containsValue("123456"));

        Assert.assertTrue(userAttributes.containsKey(HEADER_WITHOUT_ALIAS));
        Assert.assertTrue(userAttributes.containsValue("234567"));

        Assert.assertFalse(userAttributes.containsKey(UNUSED_HEADER));
    }

    @Test
    public void testHighSecurityDoesNotAddCustomHeaders() throws Exception {
        initializeTest(true);
        Map<String, Object> agentAttributes = runTransactionAndGetAttributes();

        Assert.assertFalse(agentAttributes.containsKey(HEADER_WITHOUT_ALIAS));
        Assert.assertFalse(agentAttributes.containsKey(HEADER_ALIAS));
        Assert.assertFalse(agentAttributes.containsKey(HEADER_WITH_ALIAS));
        Assert.assertFalse(agentAttributes.containsKey(UNUSED_HEADER));
    }

    private Map<String, Object> runTransactionAndGetAttributes() throws Exception {
        // setup request
        MockHttpRequest httpRequest = new MockHttpRequest()
                .setHeader("Referer", "Referer value")
                .setHeader("Accept", "Accept value")
                .setHeader("Host", "Host value")
                .setHeader("User-Agent", "User-Agent value")
                .setHeader("Content-Length", "Content-Length value")
                .setHeader(HEADER_WITH_ALIAS, "123456")
                .setHeader(HEADER_WITHOUT_ALIAS, "234567");

        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        dispatcher.getTransaction().getTransactionActivity().markAsResponseSender();
        dispatcher.getTransaction().getRootTracer().finish(0, null);

        return dispatcher.getTransaction().getUserAttributes();
    }

    private static Map<String, String> makeHeaderMap(String headerName, String headerAlias) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                .put(TransactionEventsConfig.REQUEST_HEADER_NAME, headerName);
        if (headerAlias != null) {
            builder.put(TransactionEventsConfig.HEADER_ALIAS, headerAlias);
        }
        return builder.build();
    }

    private void initializeTest(boolean isHighSecurity) throws Exception {
        List<Map<String, String>> requestHeaderList = ImmutableList.<Map<String, String>>builder()
                .add(makeHeaderMap(HEADER_WITH_ALIAS, HEADER_ALIAS))
                .add(makeHeaderMap(HEADER_WITHOUT_ALIAS, null))
                .add(makeHeaderMap(UNUSED_HEADER, null))
                .build();

        Map<String, Object> headerConfigs = new HashMap<>();
        headerConfigs.put(TransactionEventsConfig.CUSTOM_REQUEST_HEADERS, requestHeaderList);

        Map<String, Object> configMap = ImmutableMap.<String, Object>builder()
                .put(AgentConfigImpl.TRANSACTION_EVENTS, headerConfigs)
                .put(AgentConfigImpl.APP_NAME, APP_NAME)
                .put(AgentConfigImpl.HIGH_SECURITY, isHighSecurity)
                .build();

        createServiceManager(AgentConfigImpl.createAgentConfig(configMap), configMap);

        Transaction.clearTransaction();
        stats = new TransactionStats();
    }

    private static void createServiceManager(AgentConfig config, Map<String, Object> configMap) throws Exception {
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);

        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        AttributesService attributesService = new AttributesService();
        serviceManager.setAttributesService(attributesService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceManager.setRPMService(rpmService);

        configService.start();
    }

    private WebRequestDispatcher createDispatcher(MockHttpRequest httpRequest) throws Exception {
        Transaction tx = Transaction.getTransaction();
        Response httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "methodName", "()V");
        DefaultTracer tracer = new OtherRootTracer(tx, sig, this, new SimpleMetricNameFormat("test"));
        tx.getTransactionActivity().tracerStarted(tracer);
        WebRequestDispatcher dispatcher = new WebRequestDispatcher(httpRequest, httpResponse, tx);
        tx.setDispatcher(dispatcher);
        return dispatcher;
    }

}
