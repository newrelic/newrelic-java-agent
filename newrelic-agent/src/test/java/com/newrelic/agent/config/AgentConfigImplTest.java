/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Mocks;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import com.newrelic.agent.config.internal.MapEnvironmentFacade;
import com.newrelic.agent.config.internal.MapSystemProps;
import com.newrelic.bootstrap.BootstrapAgent;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.newrelic.agent.config.SpanEventsConfig.SERVER_SPAN_HARVEST_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/* (non-javadoc)
 * Note: the "beacon" was a predecessor technology for correlated transaction traces with the browser.
 * Some appearances of the term could be changed to "browser" now.
 */

public class AgentConfigImplTest {

    @After
    public void after() {
        System.getProperties().remove(BootstrapAgent.NR_AGENT_ARGS_SYSTEM_PROPERTY);
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
        new TestConfig().clearDeprecatedProps();
    }

    @Test
    public void collectorDefaultHost() {
        Map<String, Object> localMap = new HashMap<>();

        // regular pre-protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "0123456789abcdef0123456789abcdef01234567");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_HOST, config.getHost());

        // improper padding
        localMap.put(AgentConfigImpl.LICENSE_KEY, "xGOV016789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_HOST, config.getHost());

        // no padding
        localMap.put(AgentConfigImpl.LICENSE_KEY, "GOV0076789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_HOST, config.getHost());
    }

    @Test
    public void collectorRegionAwareHost() {
        Map<String, Object> localMap = new HashMap<>();

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "Us01xX6789abcdef0123456789abcdef01234567");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("collector.us01.nr-data.net", config.getHost());

        // proper 5 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "goV09x6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("collector.gov09.nr-data.net", config.getHost());

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "eu03XX6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("collector.eu03.nr-data.net", config.getHost());
    }

    @Test
    public void collectorSetHost() {
        Map<String, Object> localMap = new HashMap<>();

        // if host is set explicitly, never parse the license key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "0123456789abcdef0123456789abcdef01234567");
        localMap.put(AgentConfigImpl.HOST, "staging-collector.newrelic.com");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("staging-collector.newrelic.com", config.getHost());

        // if host is set explicitly, never parse the license key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "us01xx6789abcdef0123456789abcdef01234567");
        localMap.put(AgentConfigImpl.HOST, "staging-collector.newrelic.com");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("staging-collector.newrelic.com", config.getHost());
    }

    @Test
    public void defaultMetricIngestUri() {
        Map<String, Object> localMap = new HashMap<>();

        // regular pre-protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "0123456789abcdef0123456789abcdef01234567");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_METRIC_INGEST_URI, config.getMetricIngestUri());

        // improper padding
        localMap.put(AgentConfigImpl.LICENSE_KEY, "xEU016789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_METRIC_INGEST_URI, config.getMetricIngestUri());

        // no padding
        localMap.put(AgentConfigImpl.LICENSE_KEY, "EU0076789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_METRIC_INGEST_URI, config.getMetricIngestUri());
    }

    @Test
    public void regionAwareMetricIngestUri() {
        Map<String, Object> localMap = new HashMap<>();

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "eu01xX6789abcdef0123456789abcdef01234567");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://metric-api.eu01.newrelic.com/metric/v1", config.getMetricIngestUri());

        // proper 5 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "euV09x6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://metric-api.euv09.newrelic.com/metric/v1", config.getMetricIngestUri());

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "eu03XX6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://metric-api.eu03.newrelic.com/metric/v1", config.getMetricIngestUri());

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "jp01xX6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://metric-api.jp01.newrelic.com/metric/v1", config.getMetricIngestUri());

        // proper 5 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "jpV09x6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://metric-api.jpv09.newrelic.com/metric/v1", config.getMetricIngestUri());

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "jp03XX6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://metric-api.jp03.newrelic.com/metric/v1", config.getMetricIngestUri());
    }

    @Test
    public void setMetricIngestUri() {
        Map<String, Object> localMap = new HashMap<>();
        String stagingMetricIngestUri = "https://staging-metric-api.newrelic.com/metric/v1";

        // if host is set explicitly, never parse the license key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "0123456789abcdef0123456789abcdef01234567");
        localMap.put(AgentConfigImpl.METRIC_INGEST_URI, stagingMetricIngestUri);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(stagingMetricIngestUri, config.getMetricIngestUri());

        // if host is set explicitly, never parse the license key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "eu01xx6789abcdef0123456789abcdef01234567");
        localMap.put(AgentConfigImpl.METRIC_INGEST_URI, stagingMetricIngestUri);
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(stagingMetricIngestUri, config.getMetricIngestUri());
    }

    @Test
    public void defaultEventIngestUri() {
        Map<String, Object> localMap = new HashMap<>();

        // regular pre-protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "0123456789abcdef0123456789abcdef01234567");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_EVENT_INGEST_URI, config.getEventIngestUri());

        // improper padding
        localMap.put(AgentConfigImpl.LICENSE_KEY, "xEU016789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_EVENT_INGEST_URI, config.getEventIngestUri());

        // no padding
        localMap.put(AgentConfigImpl.LICENSE_KEY, "EU0076789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(AgentConfigImpl.DEFAULT_EVENT_INGEST_URI, config.getEventIngestUri());
    }

    @Test
    public void regionAwareEventIngestUri() {
        Map<String, Object> localMap = new HashMap<>();

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "eu01xX6789abcdef0123456789abcdef01234567");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://insights-collector.eu01.nr-data.net/v1/accounts/events", config.getEventIngestUri());

        // proper 5 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "euV09x6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://insights-collector.euv09.nr-data.net/v1/accounts/events", config.getEventIngestUri());

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "eu03XX6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://insights-collector.eu03.nr-data.net/v1/accounts/events", config.getEventIngestUri());

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "jp01xX6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://insights-collector.jp01.nr-data.net/v1/accounts/events", config.getEventIngestUri());

        // proper 5 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "jpV09x6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://insights-collector.jpv09.nr-data.net/v1/accounts/events", config.getEventIngestUri());

        // proper 4 character protocol 15 key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "jp03XX6789abcdef0123456789abcdef01234567");
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals("https://insights-collector.jp03.nr-data.net/v1/accounts/events", config.getEventIngestUri());
    }

    @Test
    public void setEventIngestUri() {
        Map<String, Object> localMap = new HashMap<>();
        String stagingEventIngestUri = "https://staging-insights-collector.newrelic.com/v1/accounts/events";

        // if host is set explicitly, never parse the license key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "0123456789abcdef0123456789abcdef01234567");
        localMap.put(AgentConfigImpl.EVENT_INGEST_URI, stagingEventIngestUri);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(stagingEventIngestUri, config.getEventIngestUri());

        // if host is set explicitly, never parse the license key
        localMap.put(AgentConfigImpl.LICENSE_KEY, "eu01xx6789abcdef0123456789abcdef01234567");
        localMap.put(AgentConfigImpl.EVENT_INGEST_URI, stagingEventIngestUri);
        config = AgentConfigImpl.createAgentConfig(localMap);
        assertEquals(stagingEventIngestUri, config.getEventIngestUri());
    }

    @Test
    public void extensionReloadAsDottedInYaml() {
        AgentConfig hasDefaultReloadModified = AgentConfigImpl.createAgentConfig(Collections.emptyMap());
        boolean defaultReloadModified = hasDefaultReloadModified.getExtensionsConfig().shouldReloadModified();

        Map<String, Object> nestedMap = Collections.singletonMap("extensions", Collections.singletonMap("reload_modified", !defaultReloadModified));
        AgentConfig nestedConfig = AgentConfigImpl.createAgentConfig(nestedMap);
        assertEquals(!defaultReloadModified, nestedConfig.getExtensionsConfig().shouldReloadModified());
    }

    @Test
    public void apiHost() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.API_HOST, "bogus");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals("bogus", config.getApiHost());
    }

    @Test
    public void apiHostDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_API_HOST, config.getApiHost());
    }

    @Test
    public void apiPort() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.API_PORT, AgentConfigImpl.DEFAULT_PORT + 1);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_PORT + 1, config.getApiPort());
    }

    @Test
    public void apiPortDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_SSL_PORT, config.getApiPort());
    }

    @Test
    public void apiPortDefaultSSL() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_SSL_PORT, config.getApiPort());
    }

    @Test
    public void proxyScheme() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.PROXY_SCHEME, "foo");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals("foo", config.getProxyScheme());
    }

    @Test
    public void proxySchemeDefault() {
        AgentConfig config = AgentConfigImpl.createAgentConfig(new HashMap<>());
        assertEquals(AgentConfigImpl.DEFAULT_PROXY_SCHEME, config.getProxyScheme());
    }

    @Test
    public void sendEnvironmentInfo() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.SEND_ENVIRONMENT_INFO, !AgentConfigImpl.DEFAULT_SEND_ENVIRONMENT_INFO);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!AgentConfigImpl.DEFAULT_SEND_ENVIRONMENT_INFO, config.isSendEnvironmentInfo());
    }

    @Test
    public void sendEnvironmentInfoDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_SEND_ENVIRONMENT_INFO, config.isSendEnvironmentInfo());
    }

    @Test
    public void apdexTInMillis() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals((long) (AgentConfigImpl.DEFAULT_APDEX_T * 1000L), config.getApdexTInMillis());
    }

    @Test
    public void apdexTInMillisForTransaction() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals((long) (AgentConfigImpl.DEFAULT_APDEX_T * 1000L),
                config.getApdexTInMillis("WebTransaction/custom/en/betting/Football"));

        Map<String, Object> keyTransactionMap = new HashMap<>();
        keyTransactionMap.put("WebTransaction/custom/en/betting/Football", Float.valueOf("1.0"));
        localMap.put(AgentConfigImpl.KEY_TRANSACTIONS, keyTransactionMap);
        config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals((1000L), config.getApdexTInMillis("WebTransaction/custom/en/betting/Football"));
        assertEquals((long) (AgentConfigImpl.DEFAULT_APDEX_T * 1000L),
                config.getApdexTInMillis("WebTransaction/custom/fr/betting/Cycling"));

        keyTransactionMap.put("WebTransaction/custom/de/betting/Chess", Float.valueOf("2.0"));
        localMap.put(AgentConfigImpl.KEY_TRANSACTIONS, keyTransactionMap);
        config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals((1000L), config.getApdexTInMillis("WebTransaction/custom/en/betting/Football"));
        assertEquals((2000L), config.getApdexTInMillis("WebTransaction/custom/de/betting/Chess"));
        assertEquals((long) (AgentConfigImpl.DEFAULT_APDEX_T * 1000L),
                config.getApdexTInMillis("WebTransaction/custom/fr/betting/Cycling"));
    }

    @Test
    public void appNames() {
        Map<String, Object> localMap = new HashMap<>();
        List<String> appNames = new ArrayList<>(1);
        appNames.add(" app1");
        localMap.put(AgentConfigImpl.APP_NAME, appNames);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(1, config.getApplicationNames().size());
        assertTrue(config.getApplicationNames().contains("app1"));
        assertEquals("app1", config.getApplicationName());
    }

    @Test
    public void appNamesMultiple() {
        Map<String, Object> localMap = new HashMap<>();
        List<String> appNames = new ArrayList<>(1);
        appNames.add(" app1");
        appNames.add("app2");
        localMap.put(AgentConfigImpl.APP_NAME, appNames);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(2, config.getApplicationNames().size());
        assertTrue(config.getApplicationNames().containsAll(Arrays.asList("app1", "app2")));
        assertEquals("app1", config.getApplicationName());
    }

    @Test
    public void appNamesDuplicates() {
        Map<String, Object> localMap = new HashMap<>();
        List<String> appNames = Arrays.asList(" app1", "app2", "app1");
        localMap.put(AgentConfigImpl.APP_NAME, appNames);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(2, config.getApplicationNames().size());
        assertTrue(config.getApplicationNames().containsAll(Arrays.asList("app1", "app2")));
        assertEquals("app1", config.getApplicationName());
    }

    @Test
    public void appNamesEmpty() {
        Map<String, Object> localMap = new HashMap<>();
        List<String> appNames = new ArrayList<>(1);
        localMap.put(AgentConfigImpl.APP_NAME, appNames);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(0, config.getApplicationNames().size());
        assertNull(config.getApplicationName());
    }

    @Test
    public void appNamesMissing() {
        AgentConfig config = AgentConfigImpl.createAgentConfig(Collections.emptyMap());

        assertEquals(0, config.getApplicationNames().size());
        assertNull(config.getApplicationName());
    }

    @Test
    public void appNamesString() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.APP_NAME, " app1 ");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(1, config.getApplicationNames().size());
        assertTrue(config.getApplicationNames().contains("app1"));
        assertEquals("app1", config.getApplicationName());
    }

    @Test
    public void appNamesStringMultiple() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.APP_NAME, " app1; app2;;app3");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(3, config.getApplicationNames().size());
        assertTrue(config.getApplicationNames().containsAll(Arrays.asList("app1", "app2", "app3")));
        assertEquals("app1", config.getApplicationName());
    }

    @Test
    public void appNamesStringDuplicates() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.APP_NAME, "app1;app2;app2");
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(2, config.getApplicationNames().size());
        assertTrue(config.getApplicationNames().containsAll(Arrays.asList("app1", "app2")));
        assertEquals("app1", config.getApplicationName());
    }

    @Test
    public void insertApiKey() {
        Map<String, Object> localMap = new HashMap<>();
        String key = "OMG IM A KEEEEEEEYYYYYYY";
        localMap.put(AgentConfigImpl.INSERT_API_KEY, key);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(key, config.getInsertApiKey());
    }

    @Test
    public void insightsApiKeyDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_INSERT_API_KEY, config.getInsertApiKey());
    }

    @Test
    public void metricsIngestUriDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_METRIC_INGEST_URI, config.getMetricIngestUri());
    }

    @Test
    public void metricsIngestUri() {
        Map<String, Object> localMap = new HashMap<>();
        String key = "OMG IM A KEEEEEEEYYYYYYY";
        localMap.put(AgentConfigImpl.METRIC_INGEST_URI, key);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(key, config.getMetricIngestUri());
    }

    @Test
    public void eventsIngestUriDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_EVENT_INGEST_URI, config.getEventIngestUri());
    }

    @Test
    public void eventsIngestUri() {
        Map<String, Object> localMap = new HashMap<>();
        String key = "OMG IM A KEEEEEEEYYYYYYY";
        localMap.put(AgentConfigImpl.EVENT_INGEST_URI, key);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(key, config.getEventIngestUri());
    }

    @Test
    public void waitForRPMConnect() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.WAIT_FOR_RPM_CONNECT, !AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT, config.waitForRPMConnect());
    }

    @Test
    public void waitForRPMConnectSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.WAIT_FOR_RPM_CONNECT;
        String val = String.valueOf(!AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.WAIT_FOR_RPM_CONNECT, AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT, config.waitForRPMConnect());
    }

    @Test
    public void waitForRPMConnectServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.WAIT_FOR_RPM_CONNECT;
        String val = String.valueOf(AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT);
        serverMap.put(AgentConfigImpl.WAIT_FOR_RPM_CONNECT, serverProp);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        assertEquals(!AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT, config.waitForRPMConnect());
    }

    @Test
    public void waitForRPMConnectDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_WAIT_FOR_RPM_CONNECT, config.waitForRPMConnect());
    }

    @Test
    public void getTransactionSizeLimit() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.TRANSACTION_SIZE_LIMIT, AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT + 1);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        int expected = (AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT + 1) * 1024;
        assertEquals(expected, config.getTransactionSizeLimit());
    }

    @Test
    public void getTransactionSizeLimitSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.TRANSACTION_SIZE_LIMIT;
        String val = String.valueOf(AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.TRANSACTION_SIZE_LIMIT, AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        int expected = (AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT + 1) * 1024;
        assertEquals(expected, config.getTransactionSizeLimit());
    }

    @Test
    public void getTransactionSizeLimitServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.TRANSACTION_SIZE_LIMIT;
        String val = String.valueOf(AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT + 1);
        serverMap.put(AgentConfigImpl.TRANSACTION_SIZE_LIMIT, serverProp);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        int expected = (AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT + 1) * 1024;
        assertEquals(expected, config.getTransactionSizeLimit());
    }

    @Test
    public void getTransactionSizeLimitDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        int expected = AgentConfigImpl.DEFAULT_TRANSACTION_SIZE_LIMIT * 1024;
        assertEquals(expected, config.getTransactionSizeLimit());
    }

    @Test
    public void isEnableAutoAppNaming() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, !AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING, config.isAutoAppNamingEnabled());
    }

    @Test
    public void isEnableAutoAppNamingSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.ENABLE_AUTO_APP_NAMING;
        String val = String.valueOf(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING, config.isAutoAppNamingEnabled());
    }

    @Test
    public void isEnableAutoAppNamingServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.ENABLE_AUTO_APP_NAMING;
        String val = String.valueOf(AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING);
        serverMap.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, serverProp);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        assertEquals(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING, config.isAutoAppNamingEnabled());
    }

    @Test
    public void isEnableAutoAppNamingDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_ENABLE_AUTO_APP_NAMING, config.isAutoAppNamingEnabled());
    }

    @Test
    public void isEnableAutoTransactionNaming() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.ENABLE_AUTO_TRANSACTION_NAMING,
                !AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING,
                config.isAutoTransactionNamingEnabled());
    }

    @Test
    public void isEnableAutoTransactionNamingSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.ENABLE_AUTO_TRANSACTION_NAMING;
        String val = String.valueOf(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.ENABLE_AUTO_TRANSACTION_NAMING,
                AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING,
                config.isAutoTransactionNamingEnabled());
    }

    @Test
    public void isEnableAutoTransactionNamingServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.ENABLE_AUTO_TRANSACTION_NAMING;
        String val = String.valueOf(AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING);
        serverMap.put(AgentConfigImpl.ENABLE_AUTO_TRANSACTION_NAMING, serverProp);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        assertEquals(!AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING,
                config.isAutoTransactionNamingEnabled());
    }

    @Test
    public void isEnableAutoTransactionNamingDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(AgentConfigImpl.DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING,
                config.isAutoTransactionNamingEnabled());
    }

    @Test
    public void getThreadProfilerConfig() {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> profilerMap = new HashMap<>();
        profilerMap.put(ThreadProfilerConfigImpl.ENABLED, !ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.THREAD_PROFILER, profilerMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.getThreadProfilerConfig().isEnabled());
    }

    @Test
    public void getThreadProfilerConfigSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = ThreadProfilerConfigImpl.SYSTEM_PROPERTY_ROOT + ThreadProfilerConfigImpl.ENABLED;
        String val = String.valueOf(!ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> profilerMap = new HashMap<>();
        profilerMap.put(ThreadProfilerConfigImpl.ENABLED, ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.THREAD_PROFILER, profilerMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.getThreadProfilerConfig().isEnabled());
    }

    @Test
    public void getThreadProfilerConfigServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = ThreadProfilerConfigImpl.SYSTEM_PROPERTY_ROOT + ThreadProfilerConfigImpl.ENABLED;
        String val = String.valueOf(ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> profilerMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        profilerMap.put(ThreadProfilerConfigImpl.ENABLED, serverProp);
        serverMap.put(AgentConfigImpl.THREAD_PROFILER, profilerMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.getThreadProfilerConfig().isEnabled());
    }

    @Test
    public void getThreadProfilerConfigDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.getThreadProfilerConfig().isEnabled());
    }

    @Test
    public void getSqlTraceConfig() {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> sqlMap = new HashMap<>();
        sqlMap.put(SqlTraceConfigImpl.ENABLED, !SqlTraceConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.SLOW_SQL, sqlMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!SqlTraceConfigImpl.DEFAULT_ENABLED, config.getSqlTraceConfig().isEnabled());
    }

    @Test
    public void getSqlTraceConfigSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = SqlTraceConfigImpl.SYSTEM_PROPERTY_ROOT + SqlTraceConfigImpl.ENABLED;
        String val = String.valueOf(!SqlTraceConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> sqlMap = new HashMap<>();
        sqlMap.put(SqlTraceConfigImpl.ENABLED, SqlTraceConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.SLOW_SQL, sqlMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!SqlTraceConfigImpl.DEFAULT_ENABLED, config.getSqlTraceConfig().isEnabled());
    }

    @Test
    public void getSqlTraceConfigServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = SqlTraceConfigImpl.SYSTEM_PROPERTY_ROOT + SqlTraceConfigImpl.ENABLED;
        String val = String.valueOf(!SqlTraceConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> sqlMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!SqlTraceConfigImpl.DEFAULT_ENABLED);
        sqlMap.put(SqlTraceConfigImpl.ENABLED, serverProp);
        localMap.put(AgentConfigImpl.SLOW_SQL, sqlMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!SqlTraceConfigImpl.DEFAULT_ENABLED, config.getSqlTraceConfig().isEnabled());
    }

    @Test
    public void getSqlTraceConfigDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(SqlTraceConfigImpl.DEFAULT_ENABLED, config.getSqlTraceConfig().isEnabled());
    }

    @Test
    public void getSqlTraceConfigDefaultSqlIdSize() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(SqlTraceConfigImpl.DEFAULT_USE_LONGER_SQL_ID, config.getSqlTraceConfig().isUsingLongerSqlId());
    }

    @Test
    public void getTransactionTracerConfig() {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.ENABLED, !TransactionTracerConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getCrossProcessConfig() {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> catMap = new HashMap<>();
        catMap.put(CrossProcessConfigImpl.ENABLED, !CrossProcessConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.CROSS_APPLICATION_TRACER, catMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!CrossProcessConfigImpl.DEFAULT_ENABLED,
                config.getCrossProcessConfig().isCrossApplicationTracing());
    }

    @Test
    public void getCrossProcessConfigDeprecated() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING, !CrossProcessConfigImpl.DEFAULT_ENABLED);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!CrossProcessConfigImpl.DEFAULT_ENABLED,
                config.getCrossProcessConfig().isCrossApplicationTracing());
    }

    @Test
    public void getCrossProcessConfigBothDeprecatedAndNot() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING, CrossProcessConfigImpl.DEFAULT_ENABLED);
        Map<String, Object> catMap = new HashMap<>();
        catMap.put(CrossProcessConfigImpl.ENABLED, !CrossProcessConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.CROSS_APPLICATION_TRACER, catMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!CrossProcessConfigImpl.DEFAULT_ENABLED,
                config.getCrossProcessConfig().isCrossApplicationTracing());
    }

    @Test
    public void getTransactionTracerConfigSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.ENABLED;
        String val = String.valueOf(!ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.ENABLED, TransactionTracerConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getTransactionTracerConfigServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.ENABLED;
        String val = String.valueOf(ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!TransactionTracerConfigImpl.DEFAULT_ENABLED);
        ttMap.put(TransactionTracerConfigImpl.ENABLED, serverProp);
        serverMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getTransactionTracerConfigDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertFalse(config.getTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getRequestTransactionTracerConfig() {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME);
        categorySet.add(requestCategoryMap);
        requestCategoryMap.put(TransactionTracerConfigImpl.ENABLED, !TransactionTracerConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getRequestTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getRequestTransactionTracerConfigSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.ENABLED;
        String val = String.valueOf(ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME);
        categorySet.add(requestCategoryMap);
        ServerProp serverProp = ServerProp.createPropObject(!TransactionTracerConfigImpl.DEFAULT_ENABLED);
        requestCategoryMap.put(TransactionTracerConfigImpl.ENABLED, serverProp);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getRequestTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getRequestTransactionTracerConfigServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.ENABLED;
        String val = String.valueOf(!ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        ttMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME);
        categorySet.add(requestCategoryMap);
        ServerProp serverProp = ServerProp.createPropObject(TransactionTracerConfigImpl.DEFAULT_ENABLED);
        requestCategoryMap.put(TransactionTracerConfigImpl.ENABLED, serverProp);
        serverMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        assertEquals(TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getRequestTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getRequestTransactionTracerConfigDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertFalse(config.getRequestTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getBackgroundTransactionTracerConfig() {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME);
        categorySet.add(requestCategoryMap);
        requestCategoryMap.put(TransactionTracerConfigImpl.ENABLED, !TransactionTracerConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getBackgroundTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getBackgroundTransactionTracerConfigSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.CATEGORY_BACKGROUND_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.ENABLED;
        String val = String.valueOf(!ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME);
        categorySet.add(requestCategoryMap);
        requestCategoryMap.put(TransactionTracerConfigImpl.ENABLED, TransactionTracerConfigImpl.DEFAULT_ENABLED);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getBackgroundTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getBackgroundTransactionTracerConfigServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.CATEGORY_BACKGROUND_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.ENABLED;
        String val = String.valueOf(ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME);
        categorySet.add(requestCategoryMap);
        ServerProp serverProp = ServerProp.createPropObject(!TransactionTracerConfigImpl.DEFAULT_ENABLED);
        requestCategoryMap.put(TransactionTracerConfigImpl.ENABLED, serverProp);
        serverMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                config.getBackgroundTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getBackgroundTransactionTracerConfigDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertFalse(config.getBackgroundTransactionTracerConfig().isEnabled());
    }

    @Test
    public void getErrorCollectorConfig() {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, !ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        errorMap.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
        localMap.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!ErrorCollectorConfigImpl.DEFAULT_ENABLED, config.getErrorCollectorConfig().isEnabled());
    }

    @Test
    public void getErrorCollectorConfigSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.ENABLED;
        String val = String.valueOf(!ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertFalse(config.getErrorCollectorConfig().isEnabled());
    }

    @Test
    public void getErrorCollectorConfigServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = ErrorCollectorConfigImpl.SYSTEM_PROPERTY_ROOT + ErrorCollectorConfigImpl.ENABLED;
        String val = String.valueOf(ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> errorMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, serverProp);
        errorMap.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
        serverMap.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(serverMap);

        assertEquals(!ErrorCollectorConfigImpl.DEFAULT_ENABLED, config.getErrorCollectorConfig().isEnabled());
    }

    @Test
    public void getErrorCollectorConfigDefault() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(ErrorCollectorConfigImpl.DEFAULT_ENABLED, config.getErrorCollectorConfig().isEnabled());
    }

    @Test
    public void getBrowserMonitoringConfig() {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> beaconMap = new HashMap<>();
        beaconMap.put(BrowserMonitoringConfigImpl.AUTO_INSTRUMENT, !BrowserMonitoringConfigImpl.DEFAULT_AUTO_INSTRUMENT);
        localMap.put(AgentConfigImpl.BROWSER_MONITORING, beaconMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!BrowserMonitoringConfigImpl.DEFAULT_AUTO_INSTRUMENT,
                config.getBrowserMonitoringConfig().isAutoInstrumentEnabled());
    }

    @Test
    public void checkSystemPropertyValueOverrideWithTrue() {
        Map<String, Object> props = new HashMap<>();
        String key = "com.newrelic.instrumentation.jcache-datastore-1.0.0";
        props.put(key, true);
        SystemPropertyProvider provider = Mocks.createSystemPropertyFlattenedProvider(props);
        SystemPropertyFactory.setSystemPropertyProvider(provider);

        Map<String, Object> newrelicproperty = new HashMap<>();
        newrelicproperty.put(key, false);
        AgentConfig config = AgentConfigImpl.createAgentConfig(newrelicproperty);

        assertEquals(true, config.getValue(key));
    }

    @Test
    public void checkSystemPropertyValueOverrideWithFalse() {
        Map<String, Object> props = new HashMap<>();
        String key = "com.newrelic.instrumentation.jcache-datastore-1.0.0";
        props.put(key, false);
        SystemPropertyProvider provider = Mocks.createSystemPropertyFlattenedProvider(props);
        SystemPropertyFactory.setSystemPropertyProvider(provider);

        Map<String, Object> newrelicproperty = new HashMap<>();
        newrelicproperty.put(key, true);
        AgentConfig config = AgentConfigImpl.createAgentConfig(newrelicproperty);

        assertEquals(false, config.getValue(key));
    }

    @Test
    public void shouldNotLogUnsetProperties() {
        TestConfig target = new TestConfig();
        target.addDeprecatedProp("my-prop", null);

        HashMap<String, Object> settings = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(settings);
        List<String> deprecationMessages = agentConfig.logDeprecatedProperties(settings);

        assertTrue(deprecationMessages.isEmpty());
    }

    @Test
    public void shouldLogPropertiesFromConfig() {
        TestConfig target = new TestConfig();
        target.addDeprecatedProp("my-prop", null);

        HashMap<String, Object> settings = new HashMap<>();
        settings.put("my-prop", "some value");
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(settings);
        List<String> deprecationMessages = agentConfig.logDeprecatedProperties(settings);

        assertTrue(deprecationMessages.contains(
                "Configuration my-prop is deprecated and will be removed in the next major version. "
                        + "It was set in the configuration file. "
                        + "This property is obsolete."
        ));
    }

    @Test
    public void shouldLogPropertiesWithNewName() {
        TestConfig target = new TestConfig();
        target.addDeprecatedProp("my-prop", "new-prop");

        HashMap<String, Object> settings = new HashMap<>();
        settings.put("my-prop", "some value");
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(settings);
        List<String> deprecationMessages = agentConfig.logDeprecatedProperties(settings);

        assertTrue(deprecationMessages.contains(
                "Configuration my-prop is deprecated and will be removed in the next major version. "
                        + "It was set in the configuration file. "
                        + "Use new-prop instead."
        ));
    }

    @Test
    public void shouldLogPropertyAfterCheckingTheMap() {
        TestConfig target = new TestConfig();
        target.addDeprecatedProp(new String[] { "my-box", "my-prop" }, new String[] { "new-box", "new-prop" });

        HashMap<String, Object> myBox = new HashMap<>();
        myBox.put("my-prop", "some-value");

        HashMap<String, Object> settings = new HashMap<>();
        settings.put("my-box", myBox);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(settings);
        List<String> deprecationMessages = agentConfig.logDeprecatedProperties(settings);

        assertTrue(deprecationMessages.contains(
                "Configuration my-box.my-prop is deprecated and will be removed in the next major version. "
                        + "It was set in the configuration file. "
                        + "Use new-box.new-prop instead."
        ));
    }

    @Test
    public void shouldLogPropertiesFromEnvironment() {
        Map<String, String> env = new HashMap<>();
        String key = "newrelic.config.my-prop";
        env.put(key, "some value");
        SystemPropertyProvider provider = Mocks.createSystemPropertyProvider(Collections.emptyMap(), env);
        SystemPropertyFactory.setSystemPropertyProvider(provider);

        TestConfig target = new TestConfig();
        target.addDeprecatedProp("my-prop", null);

        HashMap<String, Object> settings = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(settings);
        List<String> deprecationMessages = agentConfig.logDeprecatedProperties(settings);

        assertTrue(deprecationMessages.contains(
                "Configuration my-prop is deprecated and will be removed in the next major version. "
                        + "It was set in the environment. "
                        + "This property is obsolete."
        ));
    }

    @Test
    public void shouldLogPropertiesFromSystemProperties() {
        Map<String, String> env = new HashMap<>();
        String key = "newrelic.config.my-prop";
        env.put(key, "some value");
        SystemPropertyProvider provider = Mocks.createSystemPropertyProvider(env, Collections.emptyMap());
        SystemPropertyFactory.setSystemPropertyProvider(provider);

        TestConfig target = new TestConfig();
        target.addDeprecatedProp("my-prop", null);

        HashMap<String, Object> settings = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(settings);
        List<String> deprecationMessages = agentConfig.logDeprecatedProperties(settings);

        assertTrue(deprecationMessages.contains(
                "Configuration my-prop is deprecated and will be removed in the next major version. "
                        + "It was set as a system property. "
                        + "This property is obsolete."
        ));
    }

    @Test
    public void shouldSetSpanMaxSamplesWithServerProp() {
        //given
        Map<String, Object> spanHarvestConfig = new HashMap<>();
        Long harvestLimit = 1L;
        spanHarvestConfig.put("harvest_limit", harvestLimit);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(SERVER_SPAN_HARVEST_CONFIG, spanHarvestConfig);
        //when
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);
        //then
        assertEquals("Span max samples should be the harvest limit of: " + harvestLimit.intValue(), harvestLimit.intValue(),
                config.getSpanEventsConfig().getMaxSamplesStored());
    }

    @Test
    public void getApplicationLoggingConfig() {
        long NOT_DEFAULT_MAX_SAMPLES_STORED = 5000;
        Map<String, Object> subForwardingMap = new HashMap<>();
        subForwardingMap.put(ApplicationLoggingForwardingConfig.ENABLED, !ApplicationLoggingForwardingConfig.DEFAULT_ENABLED);
        subForwardingMap.put(ApplicationLoggingForwardingConfig.MAX_SAMPLES_STORED, NOT_DEFAULT_MAX_SAMPLES_STORED);

        Map<String, Object> subMetricMap = new HashMap<>();
        subMetricMap.put(ApplicationLoggingMetricsConfig.ENABLED, !ApplicationLoggingMetricsConfig.DEFAULT_ENABLED);

        Map<String, Object> subDecoratingMap = new HashMap<>();
        subDecoratingMap.put(ApplicationLoggingLocalDecoratingConfig.ENABLED, !ApplicationLoggingLocalDecoratingConfig.DEFAULT_ENABLED);

        Map<String, Object> loggingMap = new HashMap<>();
        loggingMap.put(ApplicationLoggingConfigImpl.FORWARDING, subForwardingMap);
        loggingMap.put(ApplicationLoggingConfigImpl.METRICS, subMetricMap);
        loggingMap.put(ApplicationLoggingConfigImpl.LOCAL_DECORATING, subDecoratingMap);

        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.APPLICATION_LOGGING, loggingMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(ApplicationLoggingConfigImpl.DEFAULT_ENABLED, config.getApplicationLoggingConfig().isEnabled());
        assertFalse(config.getApplicationLoggingConfig().isForwardingEnabled());
        assertEquals(NOT_DEFAULT_MAX_SAMPLES_STORED, config.getApplicationLoggingConfig().getMaxSamplesStored());
        assertFalse(config.getApplicationLoggingConfig().isMetricsEnabled());
        assertTrue(config.getApplicationLoggingConfig().isLocalDecoratingEnabled());
    }

    @Test
    public void getApplicationLoggingConfigDefaults() {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertTrue(config.getApplicationLoggingConfig().isEnabled());
        assertTrue(config.getApplicationLoggingConfig().isForwardingEnabled());
        assertEquals(ApplicationLoggingForwardingConfig.DEFAULT_MAX_SAMPLES_STORED, config.getApplicationLoggingConfig().getMaxSamplesStored());
        assertTrue(config.getApplicationLoggingConfig().isMetricsEnabled());
        assertFalse(config.getApplicationLoggingConfig().isLocalDecoratingEnabled());
    }

    @Test
    public void getApplicationLoggingConfigSystemProperty() {
        String key = ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT + ApplicationLoggingConfigImpl.ENABLED;
        String val = String.valueOf(!ApplicationLoggingConfigImpl.DEFAULT_ENABLED);

        Map<String, String> properties = new HashMap<>();
        properties.put(key, val);

        SystemPropertyProvider provider = Mocks.createSystemPropertyProvider(properties);

        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> loggingMap = new HashMap<>();
        loggingMap.put(ApplicationLoggingConfigImpl.ENABLED, provider.getSystemProperty(key));
        localMap.put(ApplicationLoggingConfigImpl.FORWARDING, loggingMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertFalse(ApplicationLoggingConfigImpl.DEFAULT_ENABLED &&
                config.getTransactionTracerConfig().isEnabled());
    }

    @Test
    public void logFilePriorityOrder() {
        // lowest priority: file property
        Map<String, Object> prop = Collections.singletonMap("log_file_name", "propval");

        AgentConfig config = AgentConfigImpl.createAgentConfig(prop);
        assertEquals("propval", config.getLogFileName());

        // next-higher priority: system property
        Properties sysProps = new Properties();
        sysProps.put("newrelic.config.log_file_name", "sysprop");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(sysProps),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        config = AgentConfigImpl.createAgentConfig(prop);
        assertEquals("sysprop", config.getLogFileName());

        // second-highest priority: standard environment variable
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(sysProps),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap(
                        "NEW_RELIC_LOG_FILE_NAME", "standard-env-var"
                ))
        ));

        config = AgentConfigImpl.createAgentConfig(prop);
        assertEquals("standard-env-var", config.getLogFileName());

        // highest priority: classic environment variable
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(sysProps),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        "NEW_RELIC_LOG_FILE_NAME", "standard-env-var",
                        "NEW_RELIC_LOG", "classic-env-var"
                ))
        ));

        config = AgentConfigImpl.createAgentConfig(prop);
        assertEquals("classic-env-var", config.getLogFileName());
    }

    private static EnvironmentFacade createEnvironmentFacade(
            Map<String, String> environment, Map<String, String> systemProps) {
        EnvironmentFacade environmentFacade = new MapEnvironmentFacade(environment);

        SystemPropertyProvider systemPropertyProvider = new SystemPropertyProvider(
                new MapSystemProps(systemProps), environmentFacade);
        SystemPropertyFactory.setSystemPropertyProvider(systemPropertyProvider);
        return environmentFacade;
    }

    private static class TestConfig extends BaseConfig {
        TestConfig() {
            super(Collections.emptyMap());
        }

        void addDeprecatedProp(String property, String newProperty) {
            BaseConfig.addDeprecatedProperty(new String[] { property }, newProperty == null ? null : new String[] { newProperty });
        }

        void addDeprecatedProp(String[] property, String[] newProperty) {
            BaseConfig.addDeprecatedProperty(property, newProperty);
        }

        void clearDeprecatedProps() {
            BaseConfig.addDeprecatedProperties = true;
            BaseConfig.clearDeprecatedProperties();
        }
    }
}
