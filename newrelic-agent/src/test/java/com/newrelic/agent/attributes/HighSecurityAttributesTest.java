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
import com.newrelic.agent.config.AgentConfigImpl;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Request parameters and message parameters should be disabled for high security mode.
 */
public class HighSecurityAttributesTest {

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

    private void enableBrowser(Map<String, Object> settings) {
        Map<String, Object> browser = new HashMap<>();
        settings.put(AgentConfigImpl.BROWSER_MONITORING, browser);
        Map<String, Object> configAtts = new HashMap<>();
        browser.put(AgentConfigImpl.ATTRIBUTES, configAtts);
        configAtts.put("enabled", Boolean.TRUE);
    }

    private void enableRequestMessageAttributes(Map<String, Object> settings) {
        Map<String, Object> atts = new HashMap<>();
        settings.put("attributes", atts);
        atts.put("include", AttributeNames.HTTP_REQUEST_STAR + ", " + AttributeNames.MESSAGE_REQUEST_STAR);
    }

    private void enableSpecificRequestMessageAttributes(Map<String, Object> settings) {
        Map<String, Object> atts = new HashMap<>();
        settings.put("attributes", atts);
        atts.put("include", "request.parameters.foo, request.parameters.bar, message.parameters.foo, message.parameters.bar");
    }

    @Test
    public void testHighSecurityDefaults() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            enableBrowser(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));

            Map<String, Object> atts = new HashMap<>();
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");

            // request and message parameters are off by default
            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many");

            AttributesService service = new AttributesService();
            verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, atts), expected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHighSecurityOffAttsEnabled() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.FALSE);
            enableBrowser(settings);
            enableRequestMessageAttributes(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));

            Map<String, Object> atts = new HashMap<>();
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");

            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many",
                    "request.parameters.foo", "request.parameters.bar", "message.parameters.foo",
                    "message.parameters.bar");

            AttributesService service = new AttributesService();
            verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, atts), expected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHighSecurityOffAttsEnabledSpecifically() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.FALSE);
            enableBrowser(settings);
            enableSpecificRequestMessageAttributes(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));

            Map<String, Object> atts = new HashMap<>();
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");

            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many",
                    "request.parameters.foo", "request.parameters.bar", "message.parameters.foo",
                    "message.parameters.bar");

            AttributesService service = new AttributesService();
            verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, atts), expected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHighSecurityOnWithOtherDefaults() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.TRUE);
            enableBrowser(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));

            Map<String, Object> atts = new HashMap<>();
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");

            // request and message parameters are off by default
            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many");

            AttributesService service = new AttributesService();
            verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, atts), expected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHighSecurityOnCaptureParamsTrue() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.TRUE);
            enableBrowser(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));

            Map<String, Object> atts = new HashMap<>();
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");

            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many");
            AttributesService service = new AttributesService();
            verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterBrowserAttributes(null, atts), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, atts), expected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHighSecurityOnAttsEnabled() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.TRUE);
            enableBrowser(settings);
            enableRequestMessageAttributes(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));

            Map<String, Object> atts = new HashMap<>();
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");

            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many");

            AttributesService service = new AttributesService();
            verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, atts), expected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHighSecurityOnAttsEnabledSpecifically() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.TRUE);
            enableBrowser(settings);
            enableSpecificRequestMessageAttributes(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));

            Map<String, Object> atts = new HashMap<>();
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");

            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many");

            AttributesService service = new AttributesService();
            verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, atts), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, atts), expected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void verifyOutput(Map<String, ?> actual, Set<String> expected) {
        Assert.assertEquals(expected.size(), actual.size());
        for (String current : expected) {
            Assert.assertTrue("The expected key " + current + " is not in the actual output", actual.containsKey(current));
        }
    }

    @Test
    public void testHighSecurityDefaultsThroughAPI() {

        Map<String, Object> settings = new HashMap<>();
        settings.put("app_name", APP_NAME);
        enableBrowser(settings);

        manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
        manager.setTransactionService(new TransactionService());
        manager.setTransactionTraceService(new TransactionTraceService());
        AttributesService service = new AttributesService();
        manager.setAttributesService(service);
        try {
            Transaction t = Transaction.getTransaction();
            BasicRequestRootTracer tracer = createDispatcherTracer();
            t.getTransactionActivity().tracerStarted(tracer);

            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            impl.addCustomParameter("abc.thread", "1");
            impl.addCustomParameter("request.many", "1");
            impl.addCustomParameter("message.many", "1");
            impl.addCustomParameter("request.parameters.foo", "1");
            impl.addCustomParameter("request.parameters.bar", "1");
            impl.addCustomParameter("message.parameters.foo", "1");
            impl.addCustomParameter("message.parameters.bar", "1");

            // request and message parameters are off by default
            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many");

            verifyOutput(service.filterErrorEventAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, t.getUserAttributes()), expected);
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Test
    public void testHighSecurityOnWithOtherDefaultsThroughAPI() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.TRUE);
            enableBrowser(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
            manager.setTransactionService(new TransactionService());
            manager.setTransactionTraceService(new TransactionTraceService());

            Transaction t = Transaction.getTransaction();
            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            impl.addCustomParameter("abc.thread", "1");
            impl.addCustomParameter("request.many", "1");
            impl.addCustomParameter("message.many", "1");
            impl.addCustomParameter("request.parameters.foo", "1");
            impl.addCustomParameter("request.parameters.bar", "1");
            impl.addCustomParameter("message.parameters.foo", "1");
            impl.addCustomParameter("message.parameters.bar", "1");

            // user attributes should be off
            Set<String> expected = new HashSet<>();

            AttributesService service = new AttributesService();
            verifyOutput(service.filterErrorEventAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, t.getUserAttributes()), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, t.getUserAttributes()), expected);
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Test
    public void testHighSecurityOnWithOtherDefaultsThroughNoticeErrorAPIThrowable() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.TRUE);
            enableBrowser(settings);

            manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
            manager.setTransactionService(new TransactionService());
            manager.setTransactionTraceService(new TransactionTraceService());

            AttributesService service = new AttributesService();
            manager.setAttributesService(service);

            Transaction t = Transaction.getTransaction();
            BasicRequestRootTracer tracer = createDispatcherTracer();
            t.getTransactionActivity().tracerStarted(tracer);

            NewRelicApiImplementation impl = new NewRelicApiImplementation();
            Map<String, String> atts = new HashMap<>();
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");
            impl.noticeError(new Throwable("hello"), atts);

            // user attributes should be off
            Set<String> expected = new HashSet<>();

            verifyOutput(service.filterErrorEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, t.getErrorAttributes()), expected);
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Test
    public void testHighSecurityOnWithOtherDefaultsThroughNoticeErrorAPIMessage() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.TRUE);
            enableBrowser(settings);

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
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");
            impl.noticeError("hello", atts);

            // user attributes should be off
            Set<String> expected = new HashSet<>();

            verifyOutput(service.filterErrorEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, t.getErrorAttributes()), expected);
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Test
    public void testHighSecurityOffWithOtherDefaultsThroughNoticeErrorAPIMessage() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("app_name", APP_NAME);
            settings.put("high_security", Boolean.FALSE);
            enableBrowser(settings);

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
            atts.put("abc.thread", "1");
            atts.put("request.many", "1");
            atts.put("message.many", "1");
            atts.put("request.parameters.foo", "1");
            atts.put("request.parameters.bar", "1");
            atts.put("message.parameters.foo", "1");
            atts.put("message.parameters.bar", "1");
            impl.noticeError("hello", atts);

            // user attributes should be off
            Set<String> expected = Sets.newHashSet("abc.thread", "request.many", "message.many");

            verifyOutput(service.filterErrorEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterBrowserAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterSpanEventAttributes(APP_NAME, t.getErrorAttributes()), expected);
            verifyOutput(service.filterTransactionSegmentAttributes(APP_NAME, t.getErrorAttributes()), expected);
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

}
