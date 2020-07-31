/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.Sets;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AttributesFilterTest {
    private static final String APP_NAME = "testing";

    @Before
    public void before() {
        ServiceFactory.setServiceManager(new MockServiceManager());
    }

    private AgentConfig getConfig(boolean enabled, Set<String> include, Set<String> exclude) {
        return getConfig(null, enabled, include, exclude, Collections.<String, Object>emptyMap());
    }

    private AgentConfig getConfig(String additionalHash, boolean enabled, Set<String> include, Set<String> exclude,
            Map<String, Object> additionalRootLeveSettings) {
        Map<String, Object> settings = new HashMap<>(additionalRootLeveSettings);
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("app_name", APP_NAME);
        if (additionalHash == null) {
            settings.put("attributes", attSettings);
            attSettings.put("enabled", enabled);
            attSettings.put("exclude", exclude);
            attSettings.put("include", include);
        } else {
            Map<String, Object> subset = new HashMap<>();
            settings.put(additionalHash, subset);
            subset.put("attributes", attSettings);
            attSettings.put("enabled", enabled);
            attSettings.put("exclude", exclude);
            attSettings.put("include", include);
        }
        return AgentConfigFactory.createAgentConfig(settings, null, null);
    }

    private AgentConfig getSpanConfig(boolean attrEnabled, Set<String> attrInclude, Set<String> attrExclude,
            boolean spanAttrEnabled, Set<String> spanAttrInclude, Set<String> spanAttrExclude) {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attrSettings = new HashMap<>();
        Map<String, Object> dtSettings = new HashMap<>();
        Map<String, Object> spanSettings = new HashMap<>();
        Map<String, Object> spanAttrSettings = new HashMap<>();
        settings.put("app_name", APP_NAME);

        settings.put("attributes", attrSettings);
        attrSettings.put("enabled", attrEnabled);
        attrSettings.put("exclude", attrExclude);
        attrSettings.put("include", attrInclude);

        settings.put(AgentConfigImpl.DISTRIBUTED_TRACING, dtSettings);
        dtSettings.put("enabled", true);

        settings.put(AgentConfigImpl.SPAN_EVENTS, spanSettings);
        spanSettings.put("collect_span_events", true);
        spanSettings.put("attributes", spanAttrSettings);
        spanAttrSettings.put("enabled", spanAttrEnabled);
        spanAttrSettings.put("exclude", spanAttrExclude);
        spanAttrSettings.put("include", spanAttrInclude);

        return AgentConfigFactory.createAgentConfig(settings, null, null);
    }

    @Test
    public void testFilterDisabled() {
        AgentConfig config = getConfig(false, Collections.<String>emptySet(), Collections.<String>emptySet());
        AttributesFilter filter = new AttributesFilter(config);
        Assert.assertFalse(filter.isAttributesEnabledForBrowser());
        Assert.assertFalse(filter.isAttributesEnabledForErrorEvents());
        Assert.assertFalse(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertFalse(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("three", "four");
        values.put("five", "six");

        Assert.assertEquals(0, filter.filterErrorEventAttributes(values).size());
        Assert.assertEquals(0, filter.filterBrowserAttributes(values).size());
        Assert.assertEquals(0, filter.filterTransactionEventAttributes(values).size());
        Assert.assertEquals(0, filter.filterTransactionTraceAttributes(values).size());
    }

    @Test
    public void testFilterEnabledNothing() {
        AgentConfig config = getConfig(true, Collections.<String>emptySet(), Collections.<String>emptySet());
        AttributesFilter filter = new AttributesFilter(config);
        Assert.assertFalse(filter.isAttributesEnabledForBrowser());
        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("three", "four");
        values.put("five", "six");
        values.put("request.parameters.foo", "111");
        values.put("message.parameters.bar", "111");
        values.put("jvm.thread_name", "111");
        values.put("cpu_time", "111");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(4, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertFalse(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));
        // zero since browser disabled by default
        Assert.assertEquals(0, filter.filterBrowserAttributes(values).size());
    }

    @Test
    public void testFilterCaseSensitive() {
        Set<String> exclude = Sets.newHashSet("password");
        AgentConfig config = getConfig(true, Collections.<String>emptySet(), exclude);
        AttributesFilter filter = new AttributesFilter(config);
        Assert.assertFalse(filter.isAttributesEnabledForBrowser());
        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("password", "two");
        values.put("Password", "four");
        values.put("passWord", "six");
        values.put("PASSWORD", "111");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(3, actual.size());
        Assert.assertFalse(actual.containsKey("password"));
        Assert.assertTrue(actual.containsKey("Password"));
        Assert.assertTrue(actual.containsKey("passWord"));
        Assert.assertTrue(actual.containsKey("PASSWORD"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(3, actual.size());
        Assert.assertFalse(actual.containsKey("password"));
        Assert.assertTrue(actual.containsKey("Password"));
        Assert.assertTrue(actual.containsKey("passWord"));
        Assert.assertTrue(actual.containsKey("PASSWORD"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(3, actual.size());
        Assert.assertFalse(actual.containsKey("password"));
        Assert.assertTrue(actual.containsKey("Password"));
        Assert.assertTrue(actual.containsKey("passWord"));
        Assert.assertTrue(actual.containsKey("PASSWORD"));
        // zero since browser disabled by default
        Assert.assertEquals(0, filter.filterBrowserAttributes(values).size());
    }

    @Test
    public void testFilterRequestIncluded() {
        Set<String> include = Sets.newHashSet("request.*");
        AgentConfig config = getConfig(true, include, Collections.<String>emptySet());
        AttributesFilter filter = new AttributesFilter(config);
        Assert.assertFalse(filter.isAttributesEnabledForBrowser());
        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertTrue(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("three", "four");
        values.put("five", "six");
        values.put("request.parameters.foo", "111");
        values.put("message.parameters.bar", "111");
        values.put("jvm.thread_name", "111");
        values.put("cpu_time", "111");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(6, actual.size());
        Assert.assertTrue(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(6, actual.size());
        Assert.assertTrue(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertFalse(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));
        // zero since browser disabled by default
        Assert.assertEquals(0, filter.filterBrowserAttributes(values).size());
    }

    @Test
    public void testFilterMessageIncluded() {
        Set<String> include = Sets.newHashSet("message.*");
        AgentConfig config = getConfig(true, include, Collections.<String>emptySet());
        AttributesFilter filter = new AttributesFilter(config);
        Assert.assertFalse(filter.isAttributesEnabledForBrowser());
        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertTrue(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("three", "four");
        values.put("five", "six");
        values.put("request.parameters.foo", "111");
        values.put("message.parameters.bar", "111");
        values.put("jvm.thread_name", "111");
        values.put("cpu_time", "111");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(6, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertTrue(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(6, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertTrue(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertTrue(actual.containsKey("message.parameters.bar"));
        Assert.assertFalse(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));
        // zero since browser disabled by default
        Assert.assertEquals(0, filter.filterBrowserAttributes(values).size());
    }

    @Test
    public void testFilterMessageForEvents() {
        Set<String> include = Sets.newHashSet("message.*");
        AgentConfig config = getConfig(AgentConfigImpl.TRANSACTION_EVENTS, true, include,
                Collections.<String>emptySet(), Collections.<String, Object>emptyMap());
        AttributesFilter filter = new AttributesFilter(config);
        Assert.assertFalse(filter.isAttributesEnabledForBrowser());
        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertTrue(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("three", "four");
        values.put("five", "six");
        values.put("request.parameters.foo", "111");
        values.put("message.parameters.bar", "111");
        values.put("jvm.thread_name", "111");
        values.put("cpu_time", "111");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertTrue(actual.containsKey("message.parameters.bar"));
        Assert.assertFalse(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));
        // zero since browser disabled by default
        Assert.assertEquals(0, filter.filterBrowserAttributes(values).size());
    }

    @Test
    public void testFilterRequestForEvents() {
        Set<String> include = Sets.newHashSet("request.param*");
        AgentConfig config = getConfig(AgentConfigImpl.TRANSACTION_EVENTS, true, include,
                Collections.<String>emptySet(), Collections.<String, Object>emptyMap());
        AttributesFilter filter = new AttributesFilter(config);
        Assert.assertFalse(filter.isAttributesEnabledForBrowser());
        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertTrue(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("three", "four");
        values.put("five", "six");
        values.put("request.parameters.foo", "111");
        values.put("message.parameters.bar", "111");
        values.put("jvm.thread_name", "111");
        values.put("cpu_time", "111");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertFalse(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));
        // zero since browser disabled by default
        Assert.assertEquals(0, filter.filterBrowserAttributes(values).size());
    }

    @Test
    public void testFilterRequestForBrowser() {
        Set<String> include = Sets.newHashSet("request.param*");
        Map<String, Object> addProps = new HashMap<>();
        AgentConfig config = getConfig(AgentConfigImpl.BROWSER_MONITORING, true, include,
                Collections.<String>emptySet(), addProps);
        AttributesFilter filter = new AttributesFilter(config);
        Assert.assertTrue(filter.isAttributesEnabledForBrowser());
        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertTrue(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("three", "four");
        values.put("five", "six");
        values.put("request.parameters.foo", "111");
        values.put("message.parameters.bar", "111");
        values.put("jvm.thread_name", "111");
        values.put("cpu_time", "111");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertTrue(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(4, actual.size());
        Assert.assertFalse(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertFalse(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));

        actual = filter.filterBrowserAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("request.parameters.foo"));
        Assert.assertFalse(actual.containsKey("message.parameters.bar"));
        Assert.assertFalse(actual.containsKey("jvm.thread_name"));
        Assert.assertTrue(actual.containsKey("cpu_time"));
    }

    @Test
    public void testFilterRequestForTransactionEvents() {
        Set<String> include = Sets.newHashSet("one*");
        Set<String> exclude = Sets.newHashSet("one.person", "one.time");
        Map<String, Object> addProps = new HashMap<>();
        AgentConfig config = getConfig(AgentConfigImpl.TRANSACTION_EVENTS, true, include, exclude, addProps);
        AttributesFilter filter = new AttributesFilter(config);

        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("one.person", "four");
        values.put("one.time", "six");
        values.put("once", "eight");
        values.put("one.event", "ten");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("one.person"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("one.person"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(3, actual.size());
        Assert.assertTrue(actual.containsKey("one.event"));
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("once"));
    }

    @Test
    public void testFilterRequestForErrorCollector() {
        Set<String> include = Sets.newHashSet("one*");
        Set<String> exclude = Sets.newHashSet("one.person", "one.time");
        Map<String, Object> addProps = new HashMap<>();
        AgentConfig config = getConfig(AgentConfigImpl.ERROR_COLLECTOR, true, include, exclude, addProps);
        AttributesFilter filter = new AttributesFilter(config);

        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("one.person", "four");
        values.put("one.time", "six");
        values.put("once", "eight");
        values.put("one.event", "ten");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(3, actual.size());
        Assert.assertTrue(actual.containsKey("one.event"));
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("once"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("one.person"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("one.person"));
    }

    @Test
    public void testFilterRequestForTransactionTrace() {
        Set<String> include = Sets.newHashSet("one*");
        Set<String> exclude = Sets.newHashSet("one.person", "one.time");
        Map<String, Object> addProps = new HashMap<>();
        AgentConfig config = getConfig(AgentConfigImpl.TRANSACTION_TRACER, true, include, exclude, addProps);
        AttributesFilter filter = new AttributesFilter(config);

        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");
        values.put("one.person", "four");
        values.put("one.time", "six");
        values.put("once", "eight");
        values.put("one.event", "ten");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("one.person"));

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(3, actual.size());
        Assert.assertTrue(actual.containsKey("one.event"));
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("once"));

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(5, actual.size());
        Assert.assertTrue(actual.containsKey("one"));
        Assert.assertTrue(actual.containsKey("one.person"));
    }

    @Test
    public void testFilterRequestForTransactionTraceOff() {
        Map<String, Object> addProps = new HashMap<>();
        AgentConfig config = getConfig(AgentConfigImpl.TRANSACTION_TRACER, false, new HashSet<String>(),
                new HashSet<String>(), addProps);
        AttributesFilter filter = new AttributesFilter(config);

        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertFalse(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(1, actual.size());

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(0, actual.size());
    }

    @Test
    public void testFilterRequestForErrorsOff() {
        Map<String, Object> addProps = new HashMap<>();
        AgentConfig config = getConfig(AgentConfigImpl.ERROR_COLLECTOR, false, new HashSet<String>(),
                new HashSet<String>(), addProps);
        AttributesFilter filter = new AttributesFilter(config);

        Assert.assertFalse(filter.isAttributesEnabledForErrorEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(0, actual.size());

        actual = filter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(1, actual.size());
    }

    @Test
    public void testFilterRequestForTransactionEventsOff() {
        Map<String, Object> addProps = new HashMap<>();
        AgentConfig config = getConfig(AgentConfigImpl.TRANSACTION_EVENTS, false, new HashSet<String>(),
                new HashSet<String>(), addProps);
        AttributesFilter filter = new AttributesFilter(config);

        Assert.assertTrue(filter.isAttributesEnabledForErrorEvents());
        Assert.assertFalse(filter.isAttributesEnabledForTransactionEvents());
        Assert.assertTrue(filter.isAttributesEnabledForTransactionTraces());

        Assert.assertFalse(filter.captureMessageParams());
        Assert.assertFalse(filter.captureRequestParams());

        Map<String, Object> values = new HashMap<>();
        values.put("one", "two");

        Map<String, ?> actual = filter.filterErrorEventAttributes(values);
        Assert.assertEquals(1, actual.size());

        actual = filter.filterTransactionEventAttributes(values);
        Assert.assertEquals(0, actual.size());
    }

    @Test
    public void testFilterAttributesAtRootLevel() {
        Set<String> include = new HashSet<>();
        Set<String> exclude = new HashSet<>();
        exclude.add("request.*");

        AgentConfig config = getConfig(true, include, exclude);
        AttributesFilter attributesFilter = new AttributesFilter(config);

        Map<String, Object> values = new HashMap<>();
        values.put(AttributeNames.REQUEST_URI, "myuri");

        Map<String, ?> filteredAttributes = attributesFilter.filterAttributes(values);
        Assert.assertEquals(0, filteredAttributes.size());

        Map<String, ?> filteredTraceAttributes = attributesFilter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(0, filteredTraceAttributes.size());

        Map<String, ?> eventAttributes = attributesFilter.filterTransactionEventAttributes(values);
        Assert.assertEquals(0, eventAttributes.size());

        Map<String, ?> browserAttributes = attributesFilter.filterBrowserAttributes(values);
        Assert.assertEquals(0, browserAttributes.size());

        Map<String, ?> errorAttributes = attributesFilter.filterErrorEventAttributes(values);
        Assert.assertEquals(0, errorAttributes.size());
    }

    @Test
    public void testFilterAttributesAtRootLevelOff() {
        AgentConfig config = getConfig(true, Collections.<String>emptySet(), Collections.<String>emptySet());
        AttributesFilter attributesFilter = new AttributesFilter(config);

        Map<String, Object> values = new HashMap<>();
        values.put(AttributeNames.REQUEST_URI, "myuri");

        Map<String, ?> filteredAttributes = attributesFilter.filterAttributes(values);
        Assert.assertEquals("myuri", filteredAttributes.get(AttributeNames.REQUEST_URI));

        Map<String, ?> filteredTraceAttributes = attributesFilter.filterTransactionTraceAttributes(values);
        Assert.assertEquals("myuri", filteredTraceAttributes.get(AttributeNames.REQUEST_URI));

        Map<String, ?> eventAttributes = attributesFilter.filterTransactionEventAttributes(values);
        Assert.assertEquals("myuri", eventAttributes.get(AttributeNames.REQUEST_URI));

        // Browser is turned off by default
        Map<String, ?> browserAttributes = attributesFilter.filterBrowserAttributes(values);
        Assert.assertEquals(0, browserAttributes.size());

        Map<String, ?> errorAttributes = attributesFilter.filterErrorEventAttributes(values);
        Assert.assertEquals("myuri", errorAttributes.get(AttributeNames.REQUEST_URI));
    }

    @Test
    public void testFilterAttributesTracesOnly() {
        Map<String, Object> addProps = new HashMap<>();
        Set<String> include = Collections.emptySet();
        Set<String> exclude = new HashSet<>();
        exclude.add(AttributeNames.REQUEST_URI);

        AgentConfig config = getConfig(AgentConfigImpl.TRANSACTION_TRACER, false, include, exclude, addProps);
        AttributesFilter attributesFilter = new AttributesFilter(config);

        Map<String, Object> values = new HashMap<>();
        values.put(AttributeNames.REQUEST_URI, "myuri");

        Map<String, ?> filteredAttributes = attributesFilter.filterAttributes(values);
        Assert.assertEquals("myuri", filteredAttributes.get(AttributeNames.REQUEST_URI));

        Map<String, ?> filteredTraceAttributes = attributesFilter.filterTransactionTraceAttributes(values);
        Assert.assertEquals(0, filteredTraceAttributes.size());

        Map<String, ?> filteredErrorAttributes = attributesFilter.filterErrorEventAttributes(values);
        Assert.assertEquals("myuri", filteredErrorAttributes.get(AttributeNames.REQUEST_URI));
    }

    @Test
    public void testSpanAttributesExclude() {
        Set<String> attrInclude = Sets.newHashSet("http.*");
        Set<String> attrExclude = new HashSet<>();
        Set<String> spanAttrInclude = new HashSet<>();
        Set<String> spanAttrExclude = Sets.newHashSet("http.url");

        AgentConfig config = getSpanConfig(true, attrInclude, attrExclude, true, spanAttrInclude, spanAttrExclude);
        AttributesFilter attributesFilter = new AttributesFilter(config);

        Assert.assertTrue(attributesFilter.isAttributesEnabledForSpanEvents());

        Map<String, Object> values = new HashMap<>();
        values.put("http.url", "www.newrelic.com");
        values.put("http.method", "GET");

        Map<String, ?> filteredAttributes = attributesFilter.filterAttributes(values);
        Assert.assertEquals(2, filteredAttributes.size());
        Assert.assertEquals(filteredAttributes.get("http.url"), "www.newrelic.com");
        Assert.assertEquals(filteredAttributes.get("http.method"), "GET");

        Map<String, ?> filteredSpanAttributes = attributesFilter.filterSpanEventAttributes(values);
        Assert.assertEquals(1, filteredSpanAttributes.size());
        Assert.assertEquals(filteredSpanAttributes.get("http.method"), "GET");
    }

}
