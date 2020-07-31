/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.NormalizationRuleConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionNamingTest {

    public static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        return map;
    }

    @Before
    public void before() throws Exception {
        createServiceManager(createConfigMap());
        Transaction.getTransaction();
        Transaction.clearTransaction();
    }

    private static void createServiceManager(Map<String, Object> map) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
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

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        NormalizationService normalizationService = new NormalizationServiceImpl();
        serviceManager.setNormalizationService(normalizationService);

        AttributesService attributeService = new AttributesService();
        serviceManager.setAttributesService(attributeService);
    }

    private void startWebTransaction(String uri) throws Exception {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setRequestURI(uri);
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        Tracer tracer = new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
        tx.getTransactionActivity().tracerStarted(tracer);
    }

    private void startOtherTransaction(String uri) throws Exception {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("", "", "");
        MetricNameFormat format = new SimpleMetricNameFormat(uri);
        Tracer tracer = new OtherRootTracer(tx, sig, this, format);
        tx.getTransactionActivity().tracerStarted(tracer);
    }

    private String setUrlRules(JSONArray urlRules) {
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        List<ConnectionListener> connectionListeners = rpmServiceManager.getConnectionListeners();
        ConnectionListener connectionListener = connectionListeners.get(0);
        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        String appName = rpmService.getApplicationName();
        Map<String, Object> data = new HashMap<>();
        data.put(NormalizationRuleConfig.URL_RULES_KEY, urlRules);
        connectionListener.connected(rpmService, AgentConfigImpl.createAgentConfig(data));
        return appName;
    }

    private String setTransactionRules(JSONArray metricRules) {
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        List<ConnectionListener> connectionListeners = rpmServiceManager.getConnectionListeners();
        ConnectionListener connectionListener = connectionListeners.get(0);
        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        String appName = rpmService.getApplicationName();
        Map<String, Object> data = new HashMap<>();
        data.put(NormalizationRuleConfig.TRANSACTION_NAME_RULES_KEY, metricRules);
        connectionListener.connected(rpmService, AgentConfigImpl.createAgentConfig(data));
        return appName;
    }

    @Test
    public void matchingUrlNormalizer() throws Exception {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "/dude/.*");
                put("replacement", "/dude/*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.FALSE);
            }
        }));
        String appName = setUrlRules(rules);
        startWebTransaction("/dude/test/man");
        Transaction tx = Transaction.getTransaction();
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(MetricNames.NORMALIZED_URI_WEB_TRANSACTION + "/dude/*",
                tx.getPriorityTransactionName().getName());
        Assert.assertEquals(TransactionNamePriority.REQUEST_URI, tx.getPriorityTransactionName().getPriority());
    }

    @Test
    public void notMatchingUrlNormalizer() throws Exception {
        String uri = "/dude/test/man";
        startWebTransaction(uri);
        Transaction tx = Transaction.getTransaction();
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(MetricNames.URI_WEB_TRANSACTION + uri, tx.getPriorityTransactionName().getName());
        Assert.assertEquals(TransactionNamePriority.REQUEST_URI, tx.getPriorityTransactionName().getPriority());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void normalizedUri() throws Exception {
        String uri = "/dude/test/man";
        startWebTransaction(uri);
        Transaction tx = Transaction.getTransaction();
        String normalizedUri = "/dude2/test2/man2";
        tx.setNormalizedUri(normalizedUri);
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(MetricNames.NORMALIZED_URI_WEB_TRANSACTION + normalizedUri,
                tx.getPriorityTransactionName().getName());
        Assert.assertEquals(TransactionNamePriority.CUSTOM_HIGH, tx.getPriorityTransactionName().getPriority());
    }

    @Test
    public void ignoreTransaction() throws Exception {
        startWebTransaction("/dude/test/man");
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName ptn = tx.getPriorityTransactionName();
        tx.setIgnore(true);
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(ptn, tx.getPriorityTransactionName());
    }

    @Test
    public void transactionNameAlreadySet() throws Exception {
        startWebTransaction("/dude/test/man");
        String normalizedUri = "/dude2/test2/man2";
        Transaction tx = Transaction.getTransaction();
        TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, normalizedUri, null, TransactionNamePriority.CUSTOM_HIGH);
        PriorityTransactionName ptn = tx.getPriorityTransactionName();
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(ptn, tx.getPriorityTransactionName());
    }

    @Test
    public void httpStatusCode414() throws Exception {
        int responseStatus = 414;
        startWebTransaction("/dude/test/man");
        Transaction tx = Transaction.getTransaction();
        tx.getWebResponse().setStatus(responseStatus);
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(MetricNames.NORMALIZED_URI_WEB_TRANSACTION + "/" + String.valueOf(responseStatus) + "/*",
                tx.getPriorityTransactionName().getName());
        Assert.assertEquals(TransactionNamePriority.STATUS_CODE, tx.getPriorityTransactionName().getPriority());
    }

    @Test
    public void httpStatusCode403() throws Exception {
        int responseStatus = 403;
        startWebTransaction("/dude/test/man");
        Transaction tx = Transaction.getTransaction();
        tx.getWebResponse().setStatus(responseStatus);
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(MetricNames.NORMALIZED_URI_WEB_TRANSACTION + "/" + String.valueOf(responseStatus) + "/*",
                tx.getPriorityTransactionName().getName());
        Assert.assertEquals(TransactionNamePriority.STATUS_CODE, tx.getPriorityTransactionName().getPriority());
    }

    @Test
    public void httpStatusCode200() throws Exception {
        int responseStatus = 200;
        String uri = "/dude/test/man";
        startWebTransaction(uri);
        Transaction tx = Transaction.getTransaction();
        tx.getWebResponse().setStatus(responseStatus);
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(MetricNames.URI_WEB_TRANSACTION + uri, tx.getPriorityTransactionName().getName());
        Assert.assertEquals(TransactionNamePriority.REQUEST_URI, tx.getPriorityTransactionName().getPriority());
    }

    @Test
    public void OtherTransaction() throws Exception {
        String uri = MetricNames.OTHER_TRANSACTION_CUSTOM + "/dude/test/man";
        startOtherTransaction(uri);
        Transaction tx = Transaction.getTransaction();
        tx.getDispatcher().setTransactionName();
        Assert.assertEquals(uri, tx.getPriorityTransactionName().getName());
        Assert.assertEquals(TransactionNamePriority.REQUEST_URI, tx.getPriorityTransactionName().getPriority());
    }

    @Test
    public void renameTransaction() throws Exception {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "WebTransaction/Uri/dude/.*");
                put("replacement", "WebTransaction/Uri/dude/*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 10);
                put("each_segment", Boolean.FALSE);
            }
        }));
        String appName = setTransactionRules(rules);
        startWebTransaction("/dude/test/man");
        Transaction tx = Transaction.getTransaction();
        tx.freezeTransactionName();
        Assert.assertEquals(MetricNames.URI_WEB_TRANSACTION + "/dude/*", tx.getPriorityTransactionName().getName());
        Assert.assertEquals(TransactionNamePriority.FROZEN, tx.getPriorityTransactionName().getPriority());
    }

    @Test
    public void renameTransactionIgnore() throws Exception {
        JSONArray rules = new JSONArray();
        rules.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "WebTransaction/Uri/dude/.*");
                put("replacement", "WebTransaction/Uri/dude/*");
                put("ignore", Boolean.TRUE);
                put("eval_order", 10);
                put("each_segment", Boolean.FALSE);
            }
        }));
        String appName = setTransactionRules(rules);
        startWebTransaction("/dude/test/man");
        Transaction tx = Transaction.getTransaction();
        tx.freezeTransactionName();
        Assert.assertTrue(tx.isIgnore());
    }

}
