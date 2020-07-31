/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.servlet;

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
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Request;
import org.junit.Test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ServletUtilsTest {

    private static final String APP_NAME = "Servlet Utils Unit Test";

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

    @Test
    public void testAddAttsNoAtts() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);

        Transaction.clearTransaction();

        Map<String, String[]> atts = new HashMap<>();
        ServletUtils.recordParameters(Transaction.getTransaction(), new TestRequest(atts));

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(0, actual.size());
    }

    @Test
    public void testAddAtts() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        HashMap<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "request.parameters.*");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);
        createServiceManager(configMap);

        Map<String, String[]> parms = new HashMap<>();
        parms.put("hello", new String[] { "one" });
        parms.put("goodbye", new String[] { "two" });
        parms.put("hi", new String[] { "three" });

        Transaction.clearTransaction();

        ServletUtils.recordParameters(Transaction.getTransaction(), new TestRequest(parms));

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(3, actual.size());
        assertEquals("one", actual.get("request.parameters.hello"));
        assertEquals("two", actual.get("request.parameters.goodbye"));
        assertEquals("three", actual.get("request.parameters.hi"));
    }

    @Test
    public void testAddAttsValueTooLarge() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        HashMap<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "request.parameters.*");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);
        createServiceManager(configMap);

        Map<String, String[]> parms = new HashMap<>();
        parms.put("hello", new String[] { "one" });
        parms.put("goodbye", new String[] { "two" });
        parms.put("hi", new String[] { "three" });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 258; i++) {
            sb.append("G");
        }
        parms.put("four", new String[] { sb.toString() });
        Transaction.clearTransaction();

        ServletUtils.recordParameters(Transaction.getTransaction(), new TestRequest(parms));

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(4, actual.size());
        assertEquals("one", actual.get("request.parameters.hello"));
        assertEquals("two", actual.get("request.parameters.goodbye"));
        assertEquals("three", actual.get("request.parameters.hi"));
        assertEquals(sb.substring(0, 255), actual.get("request.parameters.four"));
    }

    @Test
    public void testAddAttsKeyTooLarge() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        HashMap<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "request.parameters.*");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);
        createServiceManager(configMap);

        Map<String, String[]> parms = new HashMap<>();
        parms.put("hello", new String[] { "one" });
        parms.put("goodbye", new String[] { "two" });
        parms.put("hi", new String[] { "three" });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 258; i++) {
            sb.append("G");
        }
        parms.put(sb.toString(), new String[] { "four" });
        Transaction.clearTransaction();

        ServletUtils.recordParameters(Transaction.getTransaction(), new TestRequest(parms));

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(3, actual.size());
        assertEquals("one", actual.get("request.parameters.hello"));
        assertEquals("two", actual.get("request.parameters.goodbye"));
        assertEquals("three", actual.get("request.parameters.hi"));
    }

    @Test
    public void testAddAttsHighSecurity() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.HIGH_SECURITY, Boolean.TRUE);
        createServiceManager(configMap);

        Map<String, String[]> parms = new HashMap<>();
        parms.put("hello", new String[] { "one" });
        parms.put("goodbye", new String[] { "two" });
        parms.put("hi", new String[] { "three" });

        Transaction.clearTransaction();

        ServletUtils.recordParameters(Transaction.getTransaction(), new TestRequest(parms));

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(0, actual.size());
    }

    @Test
    public void testAddAttsMultValues() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        HashMap<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "request.parameters.*");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);
        createServiceManager(configMap);

        Map<String, String[]> parms = new HashMap<>();
        parms.put("hello", new String[] { "one", "two" });
        parms.put("goodbye", new String[] { "three", "four" });
        parms.put("hi", new String[] { "five", "six" });

        Transaction.clearTransaction();

        ServletUtils.recordParameters(Transaction.getTransaction(), new TestRequest(parms));

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(3, actual.size());
        assertEquals("[one, two]", actual.get("request.parameters.hello"));
        assertEquals("[three, four]", actual.get("request.parameters.goodbye"));
        assertEquals("[five, six]", actual.get("request.parameters.hi"));
    }

    @Test
    public void testAddAttsMultValuesMaxedOut() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        HashMap<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "request.parameters.*");
        configMap.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);
        createServiceManager(configMap);

        Map<String, String[]> parms = new HashMap<>();
        parms.put("hello", new String[] { "one", "two" });
        parms.put("goodbye", new String[] { "three", "four" });
        parms.put("hi", new String[] { "five", "six" });
        String[] output = new String[16];
        for (int i = 0; i < 16; i++) {
            output[i] = "QWERTYUIOPASDFGHJ";
        }
        parms.put("four", output);
        Transaction.clearTransaction();

        ServletUtils.recordParameters(Transaction.getTransaction(), new TestRequest(parms));

        Map<String, ?> actual = ServiceFactory.getAttributesService().filterErrorEventAttributes(APP_NAME,
                AttributesUtils.appendAttributePrefixes(Transaction.getTransaction().getPrefixedAgentAttributes()));
        assertEquals(4, actual.size());
        assertEquals("[one, two]", actual.get("request.parameters.hello"));
        assertEquals("[three, four]", actual.get("request.parameters.goodbye"));
        assertEquals("[five, six]", actual.get("request.parameters.hi"));
        assertEquals(255, ((String) actual.get("request.parameters.four")).length());
        assertEquals("[QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ,"
                        + " QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTYUIOPASDFGHJ, QWERTY]",
                actual.get("request.parameters.four"));
    }

    public class TestRequest implements Request {
        private final Map<String, String[]> atts;

        public TestRequest(Map<String, String[]> attributes) {
            atts = attributes;
        }

        @Override
        public HeaderType getHeaderType() {
            return null;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getRequestURI() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return atts.get(name);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(atts.keySet());
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            return atts.get(name);
        }
    }

}
