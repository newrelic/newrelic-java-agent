/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.messaging;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AttributesConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.api.agent.ApplicationNamePriority;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MessagingUtilTest {

    private static final String APP_NAME = "Unit Test";

    private static AgentConfig createConfig() {
        return AgentConfigImpl.createAgentConfig(createConfigMap());
    }

    private static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

    private static void createServiceManager(Map<String, Object> configMap) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);
        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        AttributesService attService = new AttributesService();
        serviceManager.setAttributesService(attService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
    }

    // Create a Tracer for tests that require one.
    private BasicRequestRootTracer createDispatcherTracer() {
        Transaction tx = Transaction.getTransaction();
        tx.setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE, APP_NAME);
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
    }

    /**
     * Basic positive test
     *
     * @throws Exception
     */
    @Test
    public void testAddParameters() throws Exception {
        basicParametersTestWithCapture(true);
    }

    /**
     * Basic test EXCEPT that CAPTURE_MESSAGING_PARAMS is left as default (not set to true).
     *
     * @throws Exception
     */
    @Test
    public void testAddParametersNoCapture() throws Exception {
        basicParametersTestWithCapture(false);
    }

    /**
     * Test that setIgnore() works. This requires a Tracer.
     *
     * @throws Exception
     * @see Transaction.setIgnore()
     */
    @Test
    public void testAddParametersWithIgnore() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "world");

        Transaction.clearTransaction();
        BasicRequestRootTracer rootTracer = createDispatcherTracer();
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);
        Transaction.getTransaction().setIgnore(true);

        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);
        Map<String, String> messageParameters = Transaction.getTransaction().getPrefixedAgentAttributes().get("message.parameters.");

        assertNull(messageParameters);
    }

    /**
     * Test that setIgnore() becomes a no-op when there is no Tracer.
     *
     * @throws Exception
     * @see Transaction.setIgnore()
     */
    @Test
    public void testAddParametersWithIgnoreButNoTracer() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        HashMap<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "message.parameters.*");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);

        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "world");

        Transaction.clearTransaction();
        // BasicRequestDispatcherTracer rootTracer = createDispatcherTracer();
        // Transaction.getTransaction().tracerStarted(rootTracer);
        Transaction.getTransaction().setIgnore(true);

        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);
        Map<String, String> messageParameters = Transaction.getTransaction().getPrefixedAgentAttributes().get("message.parameters.");

        // Even though we called setIgnore(), the parameter is present because
        // the transaction did not have a Tracer so the ignore was ignored. ;-)
        assertEquals("world", messageParameters.get("hello"));
    }

    /**
     * Test that the config parameter for excluding specific parameters works.
     *
     * @throws Exception
     */
    @Test
    public void testAddParametersWithExcludes() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(AttributesConfigImpl.INCLUDE, "message.parameters.*");
        attributes.put(AttributesConfigImpl.EXCLUDE, "message.parameters.ignored,message.parameters.alsoIgnored");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributes);

        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "world");
        parms.put("ignored", "trouble");
        parms.put("alsoIgnored", "alsoTrouble");

        Transaction.clearTransaction();
        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals("world", actual.get("message.parameters.hello"));
        assertEquals(null, actual.get("message.parameters.ignored"));
        assertEquals(null, actual.get("message.parameters.alsoIgnored"));
    }

    /**
     * Test that the config parameter for excluding specific parameters works.
     *
     * @throws Exception
     */
    @Test
    public void testAddParametersWithWrongExcludesConfigParam() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        // Here we set the wrong (HTTP-specific) ignore list and make sure
        // there's no interaction with message queuing:
        // ... but also set the message ignore list, and check that.
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(AttributesConfigImpl.INCLUDE, "message.parameters.*");
        attributes.put(AttributesConfigImpl.EXCLUDE, "message.parameters.hello,request.parameters.alsoIgnored,request.parameters.ignored");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributes);

        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "not this time");
        parms.put("ignored", "OK here");
        parms.put("alsoIgnored", "also OK here");

        Transaction.clearTransaction();
        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);
        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));

        assertEquals(null, actual.get("message.parameters.hello"));
        assertEquals("OK here", actual.get("message.parameters.ignored"));
        assertEquals("also OK here", actual.get("message.parameters.alsoIgnored"));
    }

    /**
     * Test that we handle the empty case correctly
     *
     * @throws Exception
     */
    @Test
    public void testAddParametersWithEmptyParameters() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();

        Transaction.clearTransaction();
        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);
        Map<String, String> messageParameters = Transaction.getTransaction().getPrefixedAgentAttributes().get("message.parameters.");

        assertNull(messageParameters);
    }

    @Test
    public void testNoInteractionWithHTTPConfigParams() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        // CAPTURE_PARAMS should not affect message queuing; the
        // default value of CAPTURE_MESSAGING_PARAMS remains false here:
        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "world");

        Transaction.clearTransaction();
        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);
        Map<String, String> messageParameters = Transaction.getTransaction().getPrefixedAgentAttributes().get("message.parameters.");

        assertNull(messageParameters);
    }

    // Shared code for basic positive test with and without the CAPTURE_MESSAGING_PARAMS setting
    private void basicParametersTestWithCapture(boolean capture) throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "message.parameters.*");
        if (capture) { // only put() if true to check that default is false
            configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);
        }
        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "world");

        Transaction.clearTransaction();
        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);
        Map<String, String> messageParameters = Transaction.getTransaction().getPrefixedAgentAttributes().get("message.parameters.");

        if (capture) {
            assertEquals("world", messageParameters.get("hello"));
        } else {
            assertNull(messageParameters);
        }
    }

    @Test
    public void testAddParametersWithKeyTooLong() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "message.parameters.*");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);
        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "one");
        parms.put("goodbye", "two");
        parms.put("hi", "three");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 258; i++) {
            sb.append("G");
        }
        parms.put(sb.toString(), "four");

        Transaction.clearTransaction();
        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(3, actual.size());
        assertEquals("one", actual.get("message.parameters.hello"));
        assertEquals("two", actual.get("message.parameters.goodbye"));
        assertEquals("three", actual.get("message.parameters.hi"));
    }

    @Test
    public void testAddParametersWithValueTooLong() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "message.parameters.*");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);
        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "one");
        parms.put("goodbye", "two");
        parms.put("hi", "three");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 258; i++) {
            sb.append("G");
        }
        parms.put("four", sb.toString());
        Transaction.clearTransaction();
        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(4, actual.size());
        assertEquals("one", actual.get("message.parameters.hello"));
        assertEquals("two", actual.get("message.parameters.goodbye"));
        assertEquals("three", actual.get("message.parameters.hi"));
        assertEquals(sb.substring(0, 255), actual.get("message.parameters.four"));
    }

    @Test
    public void testAddParametersHighSecurity() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.HIGH_SECURITY, Boolean.TRUE);
        createServiceManager(configMap);

        Map<String, String> parms = new HashMap<>();
        parms.put("hello", "one");
        parms.put("goodbye", "two");
        parms.put("hi", "three");

        Transaction.clearTransaction();
        MessagingUtil.recordParameters(Transaction.getTransaction(), parms);

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(0, actual.size());
    }

}
