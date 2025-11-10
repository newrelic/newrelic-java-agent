/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.Sets;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.api.agent.NewRelicApiImplementation;
import com.newrelic.test.marker.RequiresFork;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Category(RequiresFork.class)
public class AgentAttributeSenderTest {

    private static final String APP_NAME = "NAME";
    private MockServiceManager manager;

    @Before
    public void setup() {
        try {
            manager = new MockServiceManager();
            ServiceFactory.setServiceManager(manager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BasicRequestRootTracer createDispatcherTracer() {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
    }

    private void verifyOutput(Map<String, Object> actual, Set<String> expected) {
        Assert.assertEquals(expected.size(), actual.size());
        for (String current : expected) {
            Assert.assertTrue("The expected key " + current + " is not in the actual output",
                    actual.containsKey(current));
        }
    }

    @Test
    public void testCustomAttributesInTransaction() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);

            manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));
            manager.setTransactionService(new TransactionService());
            manager.setTransactionTraceService(new TransactionTraceService());
            manager.setAttributesService(new AttributesService());

            Transaction t = Transaction.getTransaction();
            BasicRequestRootTracer tracer = createDispatcherTracer();
            t.getTransactionActivity().tracerStarted(tracer);

            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            impl.addCustomParameter("abc.thread", "1");
            impl.addCustomParameter("request.many", "1");
            impl.addCustomParameter("message.many", "1");
            impl.addCustomParameter("message.bool", true);

            Map<String, Object> customParamMap = new HashMap<>();
            customParamMap.put("key1", "val1");
            customParamMap.put("key2", 2);
            customParamMap.put("key3", new HashMap<>());
            customParamMap.put("key4", true);
            customParamMap.put("key5", null);
            impl.addCustomParameters(customParamMap);

            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many", "key1", "key2", "key4", "message.bool");

            verifyOutput(t.getUserAttributes(), expected);

        } finally {
            Transaction.clearTransaction();
        }
    }

    private String makeLongString(String base) {
        String returnValue = "a";
        for(int i = 0; i < 10; i++) {
            returnValue += returnValue;
        }

        return base + returnValue;
    }


    @Test
    public void shouldTruncateLongAttributeValues() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);

            manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));
            manager.setTransactionService(new TransactionService());
            manager.setTransactionTraceService(new TransactionTraceService());
            manager.setAttributesService(new AttributesService());

            Transaction t = Transaction.getTransaction();
            BasicRequestRootTracer tracer = createDispatcherTracer();
            t.getTransactionActivity().tracerStarted(tracer);

            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            impl.addCustomParameter(makeLongString("ignored-key-too-long"), "vv");
            String valueVeryLong = makeLongString("v2");
            impl.addCustomParameter("truncated-single-value", valueVeryLong);

            Map<String, Object> customParamMap = new HashMap<>();
            customParamMap.put(makeLongString("ignored-key-too-long-also"), "vx");
            customParamMap.put("truncated-map-value", makeLongString("v4"));
            impl.addCustomParameters(customParamMap);

            Set<String> expected = Sets.newHashSet("truncated-single-value", "truncated-map-value");

            Map<String, Object> attribs = t.getUserAttributes();
            Assert.assertEquals(expected, attribs.keySet());

            Assert.assertEquals(255, attribs.get("truncated-single-value").toString().length());
            Assert.assertNotEquals(255, valueVeryLong.length());

            Assert.assertEquals(255, attribs.get("truncated-map-value").toString().length());
            Assert.assertNotEquals(255, customParamMap.get("truncated-map-value"));

        } finally {
            Transaction.clearTransaction();
        }
    }

    @Test
    public void testCustomAttributesOutsideTransaction() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);

            manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));
            manager.setTransactionService(new TransactionService());
            manager.setTransactionTraceService(new TransactionTraceService());

            Transaction t = Transaction.getTransaction();

            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            impl.addCustomParameter("abc.thread", "1");
            impl.addCustomParameter("request.many", "1");
            impl.addCustomParameter("message.many", "1");

            Map<String, Object> customParamMap = new HashMap<>();
            customParamMap.put("key1", "val1");
            customParamMap.put("key2", 2);
            customParamMap.put("key3", new HashMap<>());
            customParamMap.put("key4", true);
            customParamMap.put("key5", null);

            impl.addCustomParameters(customParamMap);

            // no tx - no atts
            Set<String> expected = new HashSet<>();

            verifyOutput(t.getUserAttributes(), expected);

        } finally {
            Transaction.clearTransaction();
        }
    }

    @Test
    public void testErrorAttributesTypes() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);

            manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));
            manager.setTransactionService(new TransactionService());
            manager.setTransactionTraceService(new TransactionTraceService());

            Transaction t = Transaction.getTransaction();

            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("MyNumber", 54);
            attributes.put("MyAtomicInteger", new AtomicInteger(54));
            attributes.put("MyAtomicLong", new AtomicLong(54));
            attributes.put("MyAtomicBool", new AtomicBoolean(true));

            // Invalid attribute values
            attributes.put("MyBigDecimal", BigDecimal.valueOf(10.000000));
            attributes.put("MyBigInteger", BigInteger.valueOf(10000000L));
            attributes.put("MyNaN", Double.NaN);
            attributes.put("MyPosInf", Double.POSITIVE_INFINITY);
            attributes.put("MyNegInf", Double.NEGATIVE_INFINITY);

            Exception exception = new Exception("~~ oops ~~");
            impl.noticeError(exception, attributes);

            // no tx - no atts
            Set<String> expected = new HashSet<>();

            verifyOutput(t.getUserAttributes(), expected);
        } finally {
            Transaction.clearTransaction();
        }
    }
}
