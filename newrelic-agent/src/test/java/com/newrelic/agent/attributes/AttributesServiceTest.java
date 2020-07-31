/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AttributesServiceTest {
    private static final String APP_NAME = "NAME";
    MockServiceManager manager;

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
    public void testAttributesServiceDefaultHostNameAndInstanceName() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("app_name", APP_NAME);
        settings.put("attributes", attSettings);
        attSettings.put("enabled", Boolean.TRUE);
        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("host.displayName", "display1");
        atts.put("process.instanceName", "instance1");

        Set<String> expected = new HashSet<>();
        expected.add("host.displayName");
        expected.add("process.instanceName");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);

        // browser monitoring is off by default
        expected.clear();
        verifyOutput(service.filterBrowserAttributes(null, atts), expected);
    }

    @Test
    public void testAttributesServiceOneExcludes() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("app_name", APP_NAME);
        settings.put("attributes", attSettings);
        attSettings.put("enabled", Boolean.TRUE);
        attSettings.put("exclude", "hello");
        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hello", "1");
        atts.put("onlymatchroot", "1");
        atts.put("hel", "1");

        Set<String> expected = new HashSet<>();
        expected.add("onlymatchroot");
        expected.add("hel");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);

        // browser monitoring is off by default
        expected.clear();
        verifyOutput(service.filterBrowserAttributes(null, atts), expected);
    }

    @Test
    public void testAttributesServiceExcludesMultiple() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hello, good, bye, request.*");
        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hello", "1");
        atts.put("good", "1");
        atts.put("bye", "1");
        atts.put("request", "1");
        atts.put("request.param.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");
        atts.put("tada", "1");

        Set<String> expected = new HashSet<>();
        expected.add("onlymatchroot");
        expected.add("tada");
        expected.add("request");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        // browser monitoring is off by default
        expected.clear();
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributeServiceIncludeOne() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("include", "hello");
        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hello", "1");
        atts.put("onlymatchroot", "1");
        atts.put("hel", "1");

        Set<String> expected = new HashSet<>();
        expected.add("onlymatchroot");
        expected.add("hel");
        expected.add("hello");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        // browser monitoring is off by default
        expected.clear();
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributeServiceIncludeMultiple() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("include", "hello, good, bye, request.*");
        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hello", "1");
        atts.put("good", "1");
        atts.put("bye", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hello");
        expected.add("good");
        expected.add("bye");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        // browser monitoring is off by default
        expected.clear();
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributesServiceMixIncludeExclude() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hel*, good, bye*, request");
        attSettings.put("include", "hello, good*, bye, request.*");
        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("good", "1");
        atts.put("goody", "1");
        atts.put("go", "1");
        atts.put("bye", "1");
        atts.put("byeeeee", "1");
        atts.put("request", "1");
        atts.put("request.", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hello");
        expected.add("goody");
        expected.add("go");
        expected.add("bye");
        expected.add("request.");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        // browser monitoring is off by default
        expected.clear();
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributesServiceMixBrowserOn() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hel*, good, bye*, request");
        attSettings.put("include", "hello, good*, bye, request.*");
        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        Map<String, Object> bAtts = new HashMap<>();
        browser.put("attributes", bAtts);
        bAtts.put("enabled", Boolean.TRUE);

        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("good", "1");
        atts.put("goody", "1");
        atts.put("go", "1");
        atts.put("bye", "1");
        atts.put("byeeeee", "1");
        atts.put("request", "1");
        atts.put("request.", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hello");
        expected.add("goody");
        expected.add("go");
        expected.add("bye");
        expected.add("request.");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributesServiceMixErrorOff() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hel*, good, bye*, request");
        attSettings.put("include", "hello, good*, bye, request.*");
        Map<String, Object> errors = new HashMap<>();
        settings.put("error_collector", errors);
        Map<String, Object> bAtts = new HashMap<>();
        errors.put("attributes", bAtts);
        bAtts.put("enabled", Boolean.FALSE);

        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("good", "1");
        atts.put("goody", "1");
        atts.put("go", "1");
        atts.put("bye", "1");
        atts.put("byeeeee", "1");
        atts.put("request", "1");
        atts.put("request.", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hello");
        expected.add("goody");
        expected.add("go");
        expected.add("bye");
        expected.add("request.");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        // errors should be off
        expected.clear();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributesServiceTransTraceOff() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hel*, good, bye*, request");
        attSettings.put("include", "hello, good*, bye, request.*");
        Map<String, Object> tEvents = new HashMap<>();
        settings.put("transaction_tracer", tEvents);
        Map<String, Object> bAtts = new HashMap<>();
        tEvents.put("attributes", bAtts);
        bAtts.put("enabled", Boolean.FALSE);

        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("good", "1");
        atts.put("goody", "1");
        atts.put("go", "1");
        atts.put("bye", "1");
        atts.put("byeeeee", "1");
        atts.put("request", "1");
        atts.put("request.", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hello");
        expected.add("goody");
        expected.add("go");
        expected.add("bye");
        expected.add("request.");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        // tts should be off
        expected.clear();
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributesServiceTransEventsOff() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hel*, good, bye*, request");
        attSettings.put("include", "hello, good*, bye, request.*");
        Map<String, Object> tEvents = new HashMap<>();
        settings.put("transaction_events", tEvents);
        Map<String, Object> bAtts = new HashMap<>();
        tEvents.put("attributes", bAtts);
        bAtts.put("enabled", Boolean.FALSE);

        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("good", "1");
        atts.put("goody", "1");
        atts.put("go", "1");
        atts.put("bye", "1");
        atts.put("byeeeee", "1");
        atts.put("request", "1");
        atts.put("request.", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hello");
        expected.add("goody");
        expected.add("go");
        expected.add("bye");
        expected.add("request.");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        // events should be off
        expected.clear();
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributesServiceBrowser() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        Map<String, Object> bAtts = new HashMap<>();
        browser.put("attributes", bAtts);
        bAtts.put("exclude", "hello*, request*");
        bAtts.put("include", "hello, request.params.*");
        bAtts.put("enabled", Boolean.TRUE);
        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("request", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hel");
        expected.add("hello");
        expected.add("hellooo");
        expected.add("request");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);

        Set<String> browserExpected = new HashSet<>();
        browserExpected.add("hel");
        browserExpected.add("hello");
        browserExpected.add("request.params.foo");
        browserExpected.add("onlymatchroot");

        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), browserExpected);
    }

    @Test
    public void testAttributesServiceErrors() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        Map<String, Object> browser = new HashMap<>();
        settings.put("error_collector", browser);
        Map<String, Object> bAtts = new HashMap<>();
        browser.put("attributes", bAtts);
        bAtts.put("exclude", "hello*, request*");
        bAtts.put("include", "hello, request.params.*");
        bAtts.put("enabled", Boolean.TRUE);
        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("request", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hel");
        expected.add("hello");
        expected.add("hellooo");
        expected.add("request");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);

        Set<String> errorExpected = new HashSet<>();
        errorExpected.add("hel");
        errorExpected.add("hello");
        errorExpected.add("request.params.foo");
        errorExpected.add("onlymatchroot");
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), errorExpected);

        expected.clear();
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
    }

    @Test
    public void testAttributesServiceAnalyticEvents() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        Map<String, Object> bAtts = new HashMap<>();
        browser.put("attributes", bAtts);
        bAtts.put("enabled", Boolean.TRUE);

        Map<String, Object> aes = new HashMap<>();
        settings.put("transaction_events", aes);
        Map<String, Object> tAtts = new HashMap<>();
        aes.put("attributes", tAtts);
        tAtts.put("exclude", "hello*, request*");
        tAtts.put("include", "hello, request.params.*");
        tAtts.put("enabled", Boolean.TRUE);

        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("request", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hel");
        expected.add("hello");
        expected.add("hellooo");
        expected.add("request");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();

        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);

        Set<String> eventExpected = new HashSet<>();
        eventExpected.add("hel");
        eventExpected.add("hello");
        eventExpected.add("request.params.foo");
        eventExpected.add("onlymatchroot");
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), eventExpected);
    }

    @Test
    public void testAttributesServiceMixTransactionTracer() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        Map<String, Object> bAtts = new HashMap<>();
        browser.put("attributes", bAtts);
        bAtts.put("enabled", Boolean.TRUE);

        Map<String, Object> tts = new HashMap<>();
        settings.put("transaction_tracer", tts);
        Map<String, Object> tAtts = new HashMap<>();
        tts.put("attributes", tAtts);
        tAtts.put("exclude", "hello*, request*");
        tAtts.put("include", "hello, request.params.*");
        tAtts.put("enabled", Boolean.TRUE);

        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        Map<String, Object> atts = new HashMap<>();
        atts.put("hel", "1");
        atts.put("hello", "1");
        atts.put("hellooo", "1");
        atts.put("request", "1");
        atts.put("request.params.foo", "1");
        atts.put("request.status", "1");
        atts.put("onlymatchroot", "1");

        Set<String> expected = new HashSet<>();
        expected.add("hel");
        expected.add("hello");
        expected.add("hellooo");
        expected.add("request");
        expected.add("request.params.foo");
        expected.add("request.status");
        expected.add("onlymatchroot");

        AttributesService service = new AttributesService();

        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);

        Set<String> traceExpected = new HashSet<>();
        traceExpected.add("hel");
        traceExpected.add("hello");
        traceExpected.add("request.params.foo");
        traceExpected.add("onlymatchroot");
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), traceExpected);
    }

    @Test
    public void testAttributesServiceMixEverything() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "ba*, request*");
        attSettings.put("include", "be*, request.params.*");

        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        Map<String, Object> bAtts = new HashMap<>();
        browser.put("attributes", bAtts);
        bAtts.put("enabled", Boolean.TRUE);
        bAtts.put("exclude", "bee*");
        bAtts.put("include", "baa*");

        Map<String, Object> tts = new HashMap<>();
        settings.put("transaction_tracer", tts);
        Map<String, Object> tAtts = new HashMap<>();
        tts.put("attributes", tAtts);
        tAtts.put("exclude", "baa*, beee*");
        tAtts.put("include", "bee*, baaa*");
        tAtts.put("enabled", Boolean.TRUE);

        Map<String, Object> aes = new HashMap<>();
        settings.put("transaction_events", aes);
        Map<String, Object> aAtts = new HashMap<>();
        aes.put("attributes", aAtts);
        aAtts.put("exclude", "bed*");
        aAtts.put("include", "bac*");
        aAtts.put("enabled", Boolean.TRUE);

        Map<String, Object> ers = new HashMap<>();
        settings.put("error_collector", ers);
        Map<String, Object> eAtts = new HashMap<>();
        ers.put("attributes", eAtts);
        eAtts.put("exclude", "bac*, bedd*");
        eAtts.put("include", "bed*, bacc*");
        eAtts.put("enabled", Boolean.TRUE);

        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));
        AttributesService service = new AttributesService();

        Map<String, Object> atts = new HashMap<>();
        atts.put("ba", "1");
        atts.put("baa", "1");
        atts.put("baaa", "1");
        atts.put("bbbb", "1");
        atts.put("be", "1");
        atts.put("bee", "1");
        atts.put("beee", "1");
        atts.put("bac", "1");
        atts.put("bed", "1");

        Set<String> expected = new HashSet<>();
        // ba is excluded
        expected.add("baa");
        expected.add("baaa");
        expected.add("bbbb");
        expected.add("be");
        // bee and beee excluded
        // bac excluded
        expected.add("bed");
        verifyOutput(service.filterBrowserAttributes(APP_NAME, atts), expected);

        expected = new HashSet<>();
        // ba and baa is excluded
        expected.add("baaa");
        expected.add("bbbb");
        expected.add("be");
        expected.add("bee");
        // beee is excluded
        expected.add("bed");
        // bac is excluded
        verifyOutput(service.filterTransactionTraceAttributes(APP_NAME, atts), expected);

        atts = new HashMap<>();
        atts.put("ba", "1");
        atts.put("bac", "1");
        atts.put("bacc", "1");
        atts.put("bbbb", "1");
        atts.put("be", "1");
        atts.put("bed", "1");
        atts.put("bedd", "1");
        atts.put("baa", "1");
        atts.put("bee", "1");

        expected = new HashSet<>();
        expected.add("bac");
        expected.add("bacc");
        expected.add("bbbb");
        expected.add("be");
        expected.add("bee");
        verifyOutput(service.filterTransactionEventAttributes(APP_NAME, atts), expected);

        expected = new HashSet<>();
        expected.add("bacc");
        expected.add("bbbb");
        expected.add("be");
        expected.add("bed");
        expected.add("bee");
        verifyOutput(service.filterErrorEventAttributes(APP_NAME, atts), expected);
    }

    private void verifyOutput(Map<String, ?> actual, Set<String> expected) {
        Assert.assertEquals(expected.size(), actual.size());
        for (String current : expected) {
            Assert.assertTrue("The expected key " + current + " is not in the actual output", actual.containsKey(current));
        }
    }

}
