/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.Sets;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.api.agent.NewRelicApiImplementation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoticeErrorAttributesTest {

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

    @Test
    public void testNoticeErrorAPIFirstCallWins() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
            manager.setTransactionService(new TransactionService());
            manager.setTransactionTraceService(new TransactionTraceService());

            AttributesService service = new AttributesService();
            manager.setAttributesService(service);

            RPMServiceManager mockRPMServiceManager = manager.getRPMServiceManager();
            RPMService mockRPMService = mock(RPMService.class);
            ErrorService errorService = new ErrorServiceImpl(APP_NAME);
            when(mockRPMServiceManager.getRPMService()).thenReturn(mockRPMService);
            when(mockRPMService.getErrorService()).thenReturn(errorService);

            Transaction t = Transaction.getTransaction();
            BasicRequestRootTracer tracer = createDispatcherTracer();
            t.getTransactionActivity().tracerStarted(tracer);

            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            Map<String, String> atts = new HashMap<>();
            atts.put("test.foo", "1");
            impl.noticeError("hello", atts);

            Map<String, String> atts2 = new HashMap<>();
            atts.put("test.bar", "2");
            impl.noticeError("hello", atts2);

            Set<String> expected = Sets.newHashSet("test.foo");
            verifyOutput(t.getErrorAttributes(), expected);
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Test
    public void testStringifiesAndTruncates() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
            manager.setTransactionService(new TransactionService());
            manager.setTransactionTraceService(new TransactionTraceService());
            AttributesService service = new AttributesService();
            manager.setAttributesService(service);

            RPMServiceManager mockRPMServiceManager = manager.getRPMServiceManager();
            RPMService mockRPMService = mock(RPMService.class);
            ErrorService errorService = new ErrorServiceImpl(APP_NAME);
            when(mockRPMServiceManager.getRPMService()).thenReturn(mockRPMService);
            when(mockRPMService.getErrorService()).thenReturn(errorService);

            Transaction t = Transaction.getTransaction();
            BasicRequestRootTracer tracer = createDispatcherTracer();
            t.getTransactionActivity().tracerStarted(tracer);

            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            Map<String, Object> atts = new HashMap<>();
            atts.put("test.foo", new Object() {
                @Override
                public String toString() {
                    String base = "a";
                    for(int i = 0; i < 10; i++) {
                        base += base;
                    }
                    return base;
                }
            });
            impl.noticeError("hello", atts);

            Assert.assertEquals(t.getErrorAttributes().keySet(), Sets.newHashSet("test.foo"));

            Assert.assertEquals(255, ((String) t.getErrorAttributes().get("test.foo")).length());
            Assert.assertNotEquals(255, atts.get("test.foo").toString().length());
        } finally {
            Transaction.clearTransaction();
        }
    }

    private BasicRequestRootTracer createDispatcherTracer() {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
    }

    private void verifyOutput(Map<String, ?> actual, Set<String> expected) {
        Assert.assertEquals(expected.size(), actual.size());
        for (String current : expected) {
            Assert.assertTrue("The expected key " + current + " is not in the actual output", actual.containsKey(current));
        }
    }

}
