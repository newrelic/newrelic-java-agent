/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.NormalizationRuleConfig;
import com.newrelic.agent.normalization.NormalizationService;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.api.agent.TransactionNamePriority;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NormalizedTransactionTrace {
    private static final String APP_NAME = "Normalized Txn TT Test";

    @Before
    public void before() throws Exception {
        createServiceManager(createConfigMap());
        Transaction.getTransaction();
    }

    public static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        return map;
    }

    private static void createServiceManager(Map<String, Object> map) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        AttributesService attrService = new AttributesService();
        serviceManager.setAttributesService(attrService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        NormalizationService normalizationService = new NormalizationServiceImpl();
        serviceManager.setNormalizationService(normalizationService);
    }

    private String setUrlRules(JSONArray rules) {
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        List<ConnectionListener> connectionListeners = rpmServiceManager.getConnectionListeners();
        ConnectionListener connectionListener = connectionListeners.get(0);
        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        String appName = rpmService.getApplicationName();
        Map<String, Object> data = new HashMap<>();
        data.put(NormalizationRuleConfig.METRIC_NAME_RULES_KEY, rules);
        connectionListener.connected(rpmService, AgentConfigImpl.createAgentConfig(data));
        return appName;
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void testNormalizedTransactionTraceTest() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "/amq.([0-9]*)$");
                put("replacement", "/amq.*");
                put("eval_order", 1);
            }
        }));

        setUrlRules(rulesData);

        Transaction txn = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("", "", "");
        MetricNameFormat format = new SimpleMetricNameFormat("");
        Tracer tracer = new OtherRootTracer(txn, sig, this, format);
        txn.getTransactionActivity().tracerStarted(tracer);
        MockDispatcher dispatcher = new MockDispatcher();
        txn.setDispatcher(dispatcher);

        txn.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "TEST", "/amq.1234567831415");
        tracer.finish(Opcodes.ARETURN, null);
        Assert.assertEquals("/amq.*", txn.getPriorityTransactionName().getPartialName());

    }
}
