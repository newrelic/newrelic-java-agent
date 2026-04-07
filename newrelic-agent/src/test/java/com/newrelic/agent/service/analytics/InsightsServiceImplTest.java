/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.InsightsConfigImpl;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.InsightsServiceImpl.TransactionInsights;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.test.marker.RequiresFork;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Category(RequiresFork.class)
public class InsightsServiceImplTest {

    private static final String appName = "Dude";
    private static final HarvestService harvestService = Mockito.mock(HarvestService.class);
    private static final TransactionService txService = Mockito.mock(TransactionService.class);
    private static final StatsService statsService = Mockito.mock(StatsService.class);

    @Test
    public void testHarvestableConfigure() throws Exception {
        Map<String, Object> config = createConfig(true, 2);
        config.put(AgentConfigImpl.HIGH_SECURITY, true);
        config.put(AgentConfigImpl.ASYNC_TIMEOUT, 180);
        InsightsServiceImpl insights = createService(config);

        insights.addHarvestableToService(appName);
        insights.configureHarvestables(60, 1);
        assertEquals(insights.getMaxSamplesStored(), 1);
    }

    public static InsightsServiceImpl createService(Map<String, Object> config) throws Exception {
        config = new HashMap<>(config);

        ServiceFactory.setServiceManager(Mockito.mock(ServiceManager.class));
        Mockito.when(ServiceFactory.getServiceManager().getHarvestService()).thenReturn(harvestService);

        Mockito.when(ServiceFactory.getServiceManager().getStatsService()).thenReturn(statsService);

        Mockito.when(ServiceFactory.getServiceManager().getTransactionService()).thenReturn(txService);

        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager()).thenReturn(
                Mockito.mock(RPMServiceManager.class));

        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getRPMService()).thenReturn(
                Mockito.mock(RPMService.class));

        Mockito.when(ServiceFactory.getServiceManager().getConfigService()).thenReturn(
                Mockito.mock(ConfigService.class));

        Mockito.when(ServiceFactory.getServiceManager().getConfigService().getDefaultAgentConfig()).thenReturn(
                AgentConfigImpl.createAgentConfig(config));

        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getRPMService().getApplicationName()).thenReturn(
                appName);

        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(txService.getTransaction(false)).thenReturn(transaction);

        InsightsServiceImpl insights = new InsightsServiceImpl();
        Mockito.when(ServiceFactory.getServiceManager().getInsights()).thenReturn(insights);

        insights.start();
        return insights;
    }

    @Test
    public void testHighSecurity() throws Exception {
        Map<String, Object> config = createConfig(true, 2);
        config.put(AgentConfigImpl.HIGH_SECURITY, true);
        config.put(AgentConfigImpl.ASYNC_TIMEOUT, 180);
        InsightsServiceImpl insights = createService(config);

        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(ServiceFactory.getTransactionService().getTransaction(false)).thenReturn(transaction);

        TransactionInsights txInsights = new TransactionInsights(
                AgentConfigImpl.createAgentConfig(Collections.<String, Object>emptyMap()));
        Mockito.when(transaction.getInsightsData()).thenReturn(txInsights);
        Mockito.when(transaction.getApplicationName()).thenReturn(appName);
        Mockito.when(transaction.isInProgress()).thenReturn(true);

        insights.recordCustomEvent("everything", ImmutableMap.<String, Object>of("is", 5, "awesome", true));
        insights.recordCustomEvent("class", ImmutableMap.<String, Object>of("name", "Foo", "number", 666));
        insights.recordCustomEvent("method", ImmutableMap.of("className", "Foo", "methodName",
                "getSomething"));

        MockRPMService analyticsData = new MockRPMService();
        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getRPMService(appName)).thenReturn(
                analyticsData);

        TransactionData transactionData = Mockito.mock(TransactionData.class);
        Mockito.when(transactionData.getApplicationName()).thenReturn(appName);
        Mockito.when(transactionData.getInsightsData()).thenReturn(txInsights);

        insights.transactionListener.dispatcherTransactionFinished(transactionData, null);
        insights.harvestHarvestables();

        assertEquals(0, analyticsData.getEvents().size());
        assertEquals(0, txInsights.events.size());
    }

    @Test
    public void testNoTransaction() throws Exception {
        InsightsServiceImpl insights = createService(createConfig(true, 2));

        insights.addHarvestableToService(appName);

        Mockito.verify(txService, Mockito.times(1)).addTransactionListener(insights.transactionListener);

        insights.recordCustomEvent("everything", ImmutableMap.<String, Object>of("is", 5, "awesome", true));
        insights.recordCustomEvent("class", ImmutableMap.<String, Object>of("name", "Foo", "number", 666));
        insights.recordCustomEvent("method", ImmutableMap.of("className", "Foo", "methodName",
                "getSomething"));

        // Invalid:
        insights.recordCustomEvent("invalid-type", ImmutableMap.<String, Object>of("name", "Foo", "number", 666));

        Map<String, String> m;

        m = Collections.singletonMap("key", "value");
        insights.recordCustomEvent(null, m);

        m = Collections.singletonMap(null, null);
        insights.recordCustomEvent("bothNull", m);

        m = Collections.singletonMap("key", null);
        insights.recordCustomEvent("valueNull", m);

        m = Collections.singletonMap(null, "value");
        insights.recordCustomEvent("keyNull", m);

        m = new HashMap<>();
        m.put("goodKey1", "goodValue");
        m.put("goodKey2", null);
        m.put(null, null);

        insights.recordCustomEvent("AllKeyValueCombos", m);

        MockRPMService analyticsData = new MockRPMService();
        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getOrCreateRPMService(appName)).thenReturn(
                analyticsData);

        insights.harvestHarvestables();

        assertEquals(2, analyticsData.getEvents().size());

        insights.stop();
        Mockito.verify(txService, Mockito.times(1)).removeTransactionListener(insights.transactionListener);
    }

    @Test
    public void testWithTransaction() throws Exception {
        Map<String, Object> config = createConfig(true, 2);
        config.put(AgentConfigImpl.ASYNC_TIMEOUT, 180);
        InsightsServiceImpl insights = createService(config);

        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(txService.getTransaction(false)).thenReturn(transaction);

        TransactionInsights txInsights = new TransactionInsights(
                AgentConfigImpl.createAgentConfig(Collections.<String, Object>emptyMap()));
        Mockito.when(transaction.getInsightsData()).thenReturn(txInsights);
        Mockito.when(transaction.getApplicationName()).thenReturn(appName);
        Mockito.when(transaction.isInProgress()).thenReturn(true);

        insights.recordCustomEvent("everything", ImmutableMap.<String, Object>of("is", 5, "awesome", true));
        insights.recordCustomEvent("class", ImmutableMap.<String, Object>of("name", "Foo", "number", 666));
        insights.recordCustomEvent("method", ImmutableMap.of("className", "Foo", "methodName",
                "getSomething"));

        // Invalid:
        insights.recordCustomEvent("invalid-type", ImmutableMap.<String, Object>of("name", "Foo", "number", 666));

        Map<String, String> m;

        m = Collections.singletonMap("key", "value");
        insights.recordCustomEvent(null, m);

        m = Collections.singletonMap(null, null);
        insights.recordCustomEvent("bothNull", m);

        m = Collections.singletonMap("key", null);
        insights.recordCustomEvent("valueNull", m);

        m = Collections.singletonMap(null, "value");
        insights.recordCustomEvent("keyNull", m);

        m = new HashMap<>();
        m.put("goodKey1", "goodValue");
        m.put("goodKey2", null);
        m.put(null, null);

        insights.recordCustomEvent("AllKeyValueCombos", m);

        MockRPMService analyticsData = new MockRPMService();
        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getRPMService(appName)).thenReturn(
                analyticsData);

        insights.harvestHarvestables();

        assertEquals(0, analyticsData.getEvents().size());

        assertEquals(7, txInsights.events.size());
    }

    private enum Whatever {ONE_THING, ANOTHER_THING}

    @Test
    public void testWithTransaction_JAVA2614Regression() throws Exception {
        String appName = "Dude";
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, appName);
        config.put("custom_insights_events.max_samples_stored", 2);
        config.put("token_timeout", 180);

        HarvestService harvestService = Mockito.mock(HarvestService.class);

        ServiceFactory.setServiceManager(Mockito.mock(ServiceManager.class));
        Mockito.when(ServiceFactory.getServiceManager().getHarvestService()).thenReturn(harvestService);

        TransactionService txService = Mockito.mock(TransactionService.class);
        Mockito.when(ServiceFactory.getServiceManager().getTransactionService()).thenReturn(txService);

        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager()).thenReturn(
                Mockito.mock(RPMServiceManager.class));

        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getRPMService()).thenReturn(
                Mockito.mock(RPMService.class));

        Mockito.when(ServiceFactory.getServiceManager().getConfigService()).thenReturn(
                Mockito.mock(ConfigService.class));

        Mockito.when(ServiceFactory.getServiceManager().getConfigService().getDefaultAgentConfig()).thenReturn(
                AgentConfigImpl.createAgentConfig(config));

        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getRPMService().getApplicationName()).thenReturn(
                appName);

        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(txService.getTransaction(false)).thenReturn(transaction);

        TransactionInsights txInsights = new TransactionInsights(
                AgentConfigImpl.createAgentConfig(Collections.<String, Object>emptyMap()));
        Mockito.when(transaction.getInsightsData()).thenReturn(txInsights);
        Mockito.when(transaction.getApplicationName()).thenReturn(appName);
        Mockito.when(transaction.isInProgress()).thenReturn(true);

        InsightsServiceImpl insights = new InsightsServiceImpl();
        insights.start();

        insights.recordCustomEvent("everything", ImmutableMap.<String, Object>of("is", 5, "awesome", true,
                "key1", Whatever.ONE_THING, "key2", Whatever.ANOTHER_THING));

        // JAVA-2614 - null values as recordCustomEvent arguments for eventType or the attribute Map's key/value result
        // in an NPE being thrown. The following five API calls will cause an NPE if we have reintroduced the problem.
        Map<String, String> m;

        m = Collections.singletonMap("key", "value");
        insights.recordCustomEvent(null, m);

        m = Collections.singletonMap(null, null);
        insights.recordCustomEvent("bothNull", m);

        m = Collections.singletonMap("key", null);
        insights.recordCustomEvent("valueNull", m);

        m = Collections.singletonMap(null, "value");
        insights.recordCustomEvent("keyNull", m);

        m = new HashMap<>();
        m.put("goodKey1", "goodValue");
        m.put("goodKey2", null);
        m.put(null, null);

        insights.recordCustomEvent("AllKeyValueCombos", m);

        MockRPMService analyticsData = new MockRPMService();
        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getRPMService(appName)).thenReturn(
                analyticsData);

        assertEquals(0, analyticsData.getEvents().size());

        assertEquals(5, txInsights.events.size());
        CustomInsightsEvent customEvent = txInsights.events.poll();
        assertNotNull(customEvent);
        assertEquals(Whatever.ONE_THING.toString(), customEvent.getUserAttributesCopy().get("key1"));

        Writer writer = new StringWriter();
        customEvent.writeJSONString(writer);
        String json = writer.toString();
        assertNotNull(json);
        // JAVA-2614 - through 3.33, attribute values that were not JSON types would cause the Agent
        // to generate invalid JSON. We check for this by attempting to parse the JSON. If an exception
        // occurs in the line below, we have reintroduced the problem and are generating invalid JSON.
        // The assert may be useful, but is really just to ensure the parse actually occurs. N.B.: we
        // need to make the outer JSON look like a JSON object for the GSON parser to accept it.
        json = "{ 'x':" + json + "}";
        Reader reader = new InputStreamReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(new Gson().fromJson(reader, Object.class));

    }

    @Test
    public void testTransactionHarvest() throws Exception {
        final Map<String, Object> config = createConfig(true, 2);
        InsightsServiceImpl insights = createService(config);
        insights.addHarvestableToService(appName);

        TransactionInsights txInsights = new TransactionInsights(AgentConfigImpl.createAgentConfig(config));

        txInsights.recordCustomEvent("everything", ImmutableMap.<String, Object>of("is", 5, "awesome", true));
        txInsights.recordCustomEvent("class", ImmutableMap.<String, Object>of("name", "Foo", "number", 666));
        txInsights.recordCustomEvent("method", ImmutableMap.of("className", "Foo", "methodName",
                "getSomething"));

        // Invalid:
        insights.recordCustomEvent("invalid-type", ImmutableMap.<String, Object>of("name", "Foo", "number", 666));

        Map<String, String> m;

        m = Collections.singletonMap("key", "value");
        insights.recordCustomEvent(null, m);

        m = Collections.singletonMap(null, null);
        insights.recordCustomEvent("bothNull", m);

        m = Collections.singletonMap("key", null);
        insights.recordCustomEvent("valueNull", m);

        m = Collections.singletonMap(null, "value");
        insights.recordCustomEvent("keyNull", m);

        m = new HashMap<>();
        m.put("goodKey1", "goodValue");
        m.put("goodKey2", null);
        m.put(null, null);

        insights.recordCustomEvent("AllKeyValueCombos", m);

        MockRPMService analyticsData = new MockRPMService();
        Mockito.when(ServiceFactory.getServiceManager().getRPMServiceManager().getOrCreateRPMService(appName)).thenReturn(
                analyticsData);

        TransactionData transactionData = Mockito.mock(TransactionData.class);
        Mockito.when(transactionData.getApplicationName()).thenReturn(appName);
        Mockito.when(transactionData.getInsightsData()).thenReturn(txInsights);

        insights.transactionListener.dispatcherTransactionFinished(transactionData, null);
        insights.harvestHarvestables();

        assertEquals(2, analyticsData.getEvents().size());
    }

    @Test
    public void testAnalyticsEventValid() {
        assertTrue(new CustomInsightsEvent("1", System.currentTimeMillis(),
                Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
        assertTrue(new CustomInsightsEvent("aA", System.currentTimeMillis(),
                Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
        assertTrue(new CustomInsightsEvent(" ", System.currentTimeMillis(),
                Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
        assertTrue(new CustomInsightsEvent(":", System.currentTimeMillis(),
                Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
        assertTrue(new CustomInsightsEvent("_", System.currentTimeMillis(),
                Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
        Assert.assertFalse(new CustomInsightsEvent("-", System.currentTimeMillis(),
                Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
        assertTrue(new CustomInsightsEvent(
                "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345",
                System.currentTimeMillis(), Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
        Assert.assertFalse(new CustomInsightsEvent(
                "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456",
                System.currentTimeMillis(), Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
        Assert.assertFalse(new CustomInsightsEvent(null, System.currentTimeMillis(),
                Collections.<String, Object>emptyMap(), DistributedTraceServiceImpl.nextTruncatedFloat()).isValid());
    }

    @Test
    public void testCustomEventFasterHarvest() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, EventTestHelper.APP_NAME);

        EventTestHelper.createServiceManager(config);
        String appName = ServiceFactory.getRPMService().getApplicationName();

        InsightsServiceImpl insightsService = new InsightsServiceImpl();
        ((MockServiceManager) ServiceFactory.getServiceManager()).setInsights(insightsService);

        ServiceManager serviceManager = spy(ServiceFactory.getServiceManager());
        ServiceFactory.setServiceManager(serviceManager);

        HarvestServiceImpl harvestService = spy(new HarvestServiceImpl());
        doReturn(harvestService).when(serviceManager).getHarvestService();
        doReturn(0L).when(harvestService).getInitialDelay();
        insightsService.addHarvestableToService(appName);

        insightsService.configureHarvestables(60, 10);
        assertEquals(10, insightsService.getMaxSamplesStored());

        insightsService.start();

        Map<String, Object> connectionInfo = new HashMap<>();
        Map<String, Object> eventHarvest = new HashMap<>();
        Map<String, Object> harvestLimits = new HashMap<>();
        eventHarvest.put("report_period_ms", 5000L); // 5 is the lowest allowable value
        eventHarvest.put("harvest_limits", harvestLimits);
        harvestLimits.put("custom_event_data", 1L);
        connectionInfo.put("event_harvest_config", eventHarvest);

        harvestService.startHarvestables(ServiceFactory.getRPMService(), AgentConfigImpl.createAgentConfig(connectionInfo));
        Thread.sleep(500);

        Map<String, String> m = Collections.singletonMap("key", "value");
        insightsService.recordCustomEvent("CustomEvent", m);

        Thread.sleep(5050);
        checkForEvent();
        ((MockRPMService) ServiceFactory.getRPMService()).clearEvents();
        insightsService.recordCustomEvent("CustomEvent", m);
        Thread.sleep(5050);
        checkForEvent();
    }

    public Map<String, Object> createConfig(boolean enabled, int maxSamplesStored) {
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, appName);

        Map<String, Object> insightsConfig = new HashMap<>();
        insightsConfig.put(InsightsConfigImpl.MAX_SAMPLES_STORED_PROP, maxSamplesStored);
        insightsConfig.put(InsightsConfigImpl.ENABLED_PROP, enabled);

        config.put("custom_insights_events", insightsConfig);
        return config;
    }

    public void checkForEvent() {
        StatsEngine statsEngineForHarvest = ServiceFactory.getStatsService().getStatsEngineForHarvest(EventTestHelper.APP_NAME);
        assertTrue(statsEngineForHarvest.getStats(MetricName.create(MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_CUSTOMER_SEEN)).hasData());
        assertTrue(statsEngineForHarvest.getStats(MetricName.create(MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_CUSTOMER_SENT)).hasData());
        assertEquals(1, ((MockRPMService) ServiceFactory.getRPMService()).getEvents().size());

        CustomInsightsEvent customEvent = (CustomInsightsEvent) Iterables.get(((MockRPMService) ServiceFactory.getRPMService()).getEvents(), 0);
        assertEquals("CustomEvent", customEvent.getType());
    }
}
