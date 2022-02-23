/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AttributesConfigImpl;
import com.newrelic.agent.config.BrowserMonitoringConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.transaction.PriorityTransactionName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.times;


public class BrowserTransactionStateTest {

    @Before
    public void setup() {
        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setConfigService(new MockConfigService(AgentConfigFactory.createAgentConfig(
                Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null)));
        ServiceFactory.setServiceManager(serviceManager);

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUserAttributes() throws Exception {
        createServiceManager();
        Transaction tx = Transaction.getTransaction();
        Transaction.clearTransaction();
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(0, bts.getUserAttributes().size());
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        tx.getUserAttributes().put("one", 1L);
        Map<String, Object> user = bts.getUserAttributes();
        Assert.assertNotNull(user);
        Assert.assertEquals(1, user.size());
        Assert.assertEquals(1L, user.get("one"));

        tx.getUserAttributes().put("two", 5.44);
        user = bts.getUserAttributes();
        Assert.assertNotNull(user);
        Assert.assertEquals(2, user.size());
        Assert.assertEquals(5.44, user.get("two"));
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        Transaction.clearTransaction();
        tx = Transaction.getTransaction();
        bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(0, bts.getUserAttributes().size());

        tx.getUserAttributes().put("one", "abc123");
        user = bts.getUserAttributes();
        Assert.assertNotNull(user);
        Assert.assertEquals(1, user.size());
        Assert.assertEquals("abc123", user.get("one"));
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        tx.getUserAttributes().put("two", 989);
        user = bts.getUserAttributes();
        Assert.assertNotNull(user);
        Assert.assertEquals(2, user.size());
        Assert.assertEquals(989, user.get("two"));
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        tx.getUserAttributes().put("three", "hello");
        user = bts.getUserAttributes();
        Assert.assertNotNull(user);
        Assert.assertEquals(3, user.size());
        Assert.assertEquals("hello", user.get("three"));

        Transaction.clearTransaction();
    }

    @Test
    public void testAgentAttributes() throws Exception {
        createServiceManager();
        Transaction tx = Transaction.getTransaction();
        Transaction.clearTransaction();
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        tx.getAgentAttributes().put("one", 1L);
        Map<String, Object> agentAtts = bts.getAgentAttributes();
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(1, agentAtts.size());
        Assert.assertEquals(1L, agentAtts.get("one"));

        tx.getAgentAttributes().put("two", 5.44);
        agentAtts = bts.getAgentAttributes();
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(2, agentAtts.size());
        Assert.assertEquals(5.44, agentAtts.get("two"));

        Transaction.clearTransaction();
        tx = Transaction.getTransaction();
        bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        tx.getAgentAttributes().put("one", "abc123");
        agentAtts = bts.getAgentAttributes();
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(1, agentAtts.size());
        Assert.assertEquals("abc123", agentAtts.get("one"));

        tx.getAgentAttributes().put("two", 989);
        agentAtts = bts.getAgentAttributes();
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(2, agentAtts.size());
        Assert.assertEquals(989, agentAtts.get("two"));

        tx.getAgentAttributes().put("three", "hello");
        agentAtts = bts.getAgentAttributes();
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(3, agentAtts.size());
        Assert.assertEquals("hello", agentAtts.get("three"));

        Transaction.clearTransaction();
        tx = Transaction.getTransaction();
        bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        Map<String, String> requests = new HashMap<>();
        requests.put("one", "abc123");
        requests.put("two", "333");
        tx.getPrefixedAgentAttributes().put("request.parameters.", requests);
        tx.getAgentAttributes().put("three", 44);
        agentAtts = bts.getAgentAttributes();
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(3, agentAtts.size());
        Assert.assertEquals("abc123", agentAtts.get("request.parameters.one"));
        Assert.assertEquals("333", agentAtts.get("request.parameters.two"));
        Assert.assertEquals(44, agentAtts.get("three"));
        Assert.assertEquals(0, bts.getUserAttributes().size());

        Transaction.clearTransaction();
    }

    @Test
    public void testGetAttributes() throws Exception {
        createServiceManager();
        Transaction tx = Transaction.getTransaction();
        Transaction.clearTransaction();
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(0, bts.getUserAttributes().size());
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        // user
        tx.getUserAttributes().put("one", 1L);
        tx.getUserAttributes().put("two", 2.22);
        // agent
        Map<String, String> requests = new HashMap<>();
        requests.put("one", "abc123");
        requests.put("two", "ringing");
        tx.getPrefixedAgentAttributes().put("request.parameters.", requests);
        tx.getAgentAttributes().put("one", 44);
        tx.getAgentAttributes().put("two", 44.44);

        Map<String, Object> userAtts = bts.getUserAttributes();
        Map<String, Object> agentAtts = bts.getAgentAttributes();

        Assert.assertNotNull(userAtts);
        Assert.assertEquals(2, userAtts.size());
        Assert.assertEquals(1L, userAtts.get("one"));
        Assert.assertEquals(2.22, (Double) userAtts.get("two"), .001);

        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(4, agentAtts.size());
        Assert.assertEquals("abc123", agentAtts.get("request.parameters.one"));
        Assert.assertEquals("ringing", agentAtts.get("request.parameters.two"));
        Assert.assertEquals(44, agentAtts.get("one"));
        Assert.assertEquals(44.44, (Double) agentAtts.get("two"), .001);

        Transaction.clearTransaction();
    }

    public static void createServiceManager() throws Exception {
        createServiceManager(Collections.<String>emptySet(), Collections.<String>emptySet());
    }

    public static void createServiceManager(Set<String> include, Set<String> exclude) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        // Needed by TransactionService
        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        // Needed by TransactionTraceService
        Map<String, Object> map = createConfigMap(include, exclude);
        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        // Needed by Transaction
        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);
        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        // Null pointers if not set
        serviceManager.setStatsService(Mockito.mock(StatsService.class));

        // Needed by Transaction
        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        // Needed by Transaction
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName("name");
        rpmService.setErrorService(new ErrorServiceImpl("name"));
        rpmServiceManager.setRPMService(rpmService);

        AttributesService attService = new AttributesService();
        serviceManager.setAttributesService(attService);

        ServiceFactory.setServiceManager(serviceManager);
    }

    private static Map<String, Object> createConfigMap(Set<String> include, Set<String> exclude) {
        return ImmutableMap.<String, Object>of(
                AgentConfigImpl.APP_NAME, "name",
                AgentConfigImpl.APDEX_T, 0.5f,
                AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE,
                AgentConfigImpl.TRANSACTION_TRACER, ImmutableMap.<String, Object>of(
                        TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f),
                AgentConfigImpl.BROWSER_MONITORING, ImmutableMap.<String, Object>of(
                        AgentConfigImpl.ATTRIBUTES, ImmutableMap.of(
                                AttributesConfigImpl.ENABLED, Boolean.TRUE,
                                AttributesConfigImpl.INCLUDE, include,
                                AttributesConfigImpl.EXCLUDE, exclude)));
    }

    @Test
    public void allowMultipleFootersDisabled() throws Exception {
        BrowserTransactionState bts = mockMultipleFootersTest(false);

        Assert.assertEquals("header", bts.getBrowserTimingHeader());
        Assert.assertEquals("footer", bts.getBrowserTimingFooter());
        Assert.assertEquals("", bts.getBrowserTimingFooter());

        Mockito.verify(tx, times(1)).freezeTransactionName();
    }

    @Test
    public void allowMultipleFootersWithNonceDisabled() {
        BrowserTransactionState bts = mockMultipleFootersTest(false);

        Assert.assertEquals("header", bts.getBrowserTimingHeader());
        Assert.assertEquals("footerWithNonce", bts.getBrowserTimingFooter("ABC123"));
        Assert.assertEquals("", bts.getBrowserTimingFooter("ABC123"));

        Mockito.verify(tx, times(1)).freezeTransactionName();
    }

    @Test
    public void allowMultipleFootersEnabled() throws Exception {
        BrowserTransactionState bts = mockMultipleFootersTest(true);

        Assert.assertEquals("header", bts.getBrowserTimingHeader());
        Assert.assertEquals("footer", bts.getBrowserTimingFooter());
        Assert.assertEquals("footer", bts.getBrowserTimingFooter());

        Mockito.verify(tx, times(2)).freezeTransactionName();
    }

    @Test
    public void allowMultipleFootersWithNonceEnabled() throws Exception {
        BrowserTransactionState bts = mockMultipleFootersTest(true);

        Assert.assertEquals("header", bts.getBrowserTimingHeader());
        Assert.assertEquals("footerWithNonce", bts.getBrowserTimingFooter("ABC123"));
        Assert.assertEquals("footerWithNonce", bts.getBrowserTimingFooter("ABC123"));

        Mockito.verify(tx, times(2)).freezeTransactionName();
    }

    @Test
    public void allowMultipleFootersMixed() throws Exception {
        BrowserTransactionState bts = mockMultipleFootersTest(true);

        Assert.assertEquals("header", bts.getBrowserTimingHeader());
        Assert.assertEquals("footer", bts.getBrowserTimingFooter());
        Assert.assertEquals("footerWithNonce", bts.getBrowserTimingFooter("ABC123"));

        Mockito.verify(tx, times(2)).freezeTransactionName();
    }

    private BrowserTransactionState mockMultipleFootersTest(boolean allowMultipleFooters) {
        PriorityTransactionName ptn = PriorityTransactionName.create("/en/betting/Football", null,
                TransactionNamePriority.CUSTOM_HIGH);
        AgentConfig agentConfig = Mockito.mock(AgentConfig.class);
        BrowserMonitoringConfig bmConfig = Mockito.mock(BrowserMonitoringConfig.class);
        Mockito.when(bmConfig.isAllowMultipleFooters()).thenReturn(allowMultipleFooters);
        Mockito.when(agentConfig.getBrowserMonitoringConfig()).thenReturn(bmConfig);

        Mockito.when(tx.isInProgress()).thenReturn(true);
        Mockito.when(tx.isIgnore()).thenReturn(false);
        Mockito.when(tx.getApplicationName()).thenReturn("Test");
        Mockito.when(tx.getPriorityTransactionName()).thenReturn(ptn);
        Mockito.when(tx.getAgentConfig()).thenReturn(agentConfig);
        Mockito.doNothing().when(tx).freezeTransactionName();
        long durationInNanos = TimeUnit.NANOSECONDS.convert(200L, TimeUnit.MILLISECONDS);
        Mockito.when(tx.getRunningDurationInNanos()).thenReturn(durationInNanos);

        final BrowserConfig bConfig = Mockito.mock(BrowserConfig.class);

        BrowserTransactionState bts = new BrowserTransactionStateImpl(tx) {
            @Override
            protected BrowserConfig getBeaconConfig() {
                return bConfig;
            }
        };

        Mockito.when(bConfig.getBrowserTimingHeader()).thenReturn("header");
        Mockito.when(bConfig.getBrowserTimingFooter(bts)).thenReturn("footer");
        Mockito.when(bConfig.getBrowserTimingFooter(eq(bts), anyString())).thenReturn("footerWithNonce");
        return bts;
    }

    @Mock
    Transaction tx;
}
