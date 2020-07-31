/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class DatastoreInstanceCrossAgentTests {

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection<Object[]> getParameters() throws Exception {
        JSONArray jsonTests = CrossAgentInput.readJsonAndGetTests("com/newrelic/agent/cross_agent_tests/datastores/datastore_instances.json");
        List<Object[]> tests = new LinkedList<>();

        for(Object jsonTest : jsonTests) {
            final DatastoreInstanceCrossAgentTest test = new DatastoreInstanceCrossAgentTest((JSONObject) jsonTest);
            tests.add(new Object[] {
                    test.getTestName(),
                    test
            });
        }

        return tests;
    }

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public DatastoreInstanceCrossAgentTest test;

    MockServiceManager manager;

    @Before
    public void setup() throws Exception {
        AgentHelper.initializeConfig();
        manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);
        manager.setConfigService(ConfigServiceFactory.createConfigService(mock(Logger.class), false));
        TransactionService ts = Mockito.mock(TransactionService.class);
        manager.setTransactionService(ts);
        TransactionTraceService tts = Mockito.mock(TransactionTraceService.class);
        manager.setTransactionTraceService(tts);
        manager.setStatsService(new StatsServiceImpl());
    }

    @Test
    public void runTest() {
        // Setup system host
        DatastoreMetrics.setHostname(test.getSystemHostname());

        DefaultTracer tracer = createTracerInTransaction();
        ExternalParameters parameters = null;

        if (test.getPort() != null) {
            parameters = DatastoreParameters
                    .product(test.getProduct())
                    .collection(test.getProduct())
                    .operation("operation")
                    .instance(test.getDbHostname(), test.getPort())
                    .build();
        } else {
            parameters = DatastoreParameters
                    .product(test.getProduct())
                    .collection("collection")
                    .operation("operation")
                    .instance(test.getDbHostname(), test.getDbPath() != null ? test.getDbPath() : test.getUnixSocket())
                    .databaseName("databaseName")
                    .slowQuery("raw query", null)
                    .build();
        }

        tracer.reportAsExternal(parameters);
        tracer.finish(Opcodes.ARETURN, null);

        Set<String> rollupMetricNames = tracer.getRollupMetricNames();
        assertTrue(
                MessageFormat.format("Could not find instance metric {0}", test.getExpectedInstanceMetric()),
                rollupMetricNames.contains(test.getExpectedInstanceMetric())
        );
    }

    private DefaultTracer createTracerInTransaction() {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "method", "()V");
        DefaultTracer tracer = new OtherRootTracer(tx, sig, this, new SimpleMetricNameFormat("test"));
        tx.getTransactionActivity().tracerStarted(tracer);
        tx.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true, "custom", "foo");
        Assert.assertEquals(0, AgentHelper.getChildren(tracer).size());
        return tracer;
    }

}
