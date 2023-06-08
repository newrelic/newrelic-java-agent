/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.BoundTransactionApiImpl;
import com.newrelic.agent.HeadersUtil;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpToken;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.DistributedTracePayloadParser;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.tracing.W3CTraceParentHeader;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.agent.util.TimeConversion;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import org.json.simple.JSONArray;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.newrelic.agent.AgentHelper.getFullPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LegacyTracerTest {

    private String APP_NAME;

    @BeforeClass
    public static void beforeClass() throws Exception {
        String configPath = getFullPath("/com/newrelic/agent/config/span_events.yml");
        System.setProperty("newrelic.config.file", configPath);
        Agent.disableFastPath();

        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    @Before
    public void before() throws Exception {
        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        APP_NAME = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        eventPool.clear();
    }

    @Test
    public void testCreateTracer() throws URISyntaxException {
        testCreateTracer(false);
    }

    @Test
    public void testCreateSqlTracer() throws URISyntaxException {
        testCreateTracer(true);
    }

    private void testCreateTracer(boolean isSqlTracer) throws URISyntaxException {
        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        TransactionActivity txa = TransactionActivity.get();
        Tracer root = new OtherRootTracer(tx, new ClassMethodSignature("com.newrelic.agent.TracedActivityTest",
                "makeTransaction", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txa.tracerStarted(root);

        final Token token = tx.getToken();

        TransactionActivity.clear();
        Transaction.clearTransaction();

        final AgentBridge.TokenAndRefCount tokenAndRefCount = new AgentBridge.TokenAndRefCount(token, root, new AtomicInteger(1));
        AgentBridge.activeToken.set(tokenAndRefCount);

        DefaultTracer tracer = (DefaultTracer)
                (isSqlTracer ?
                        AgentBridge.instrumentation.createSqlTracer(null, 0,
                                "iamyourchild", DefaultTracer.DEFAULT_TRACER_FLAGS) :
                        AgentBridge.instrumentation.createTracer(null, 0,
                                "iamyourchild", DefaultTracer.DEFAULT_TRACER_FLAGS));

        Assert.assertNotNull(tracer);

        root.finish(0, null);
        assertClmAbsent(root);
        assertClmAbsent(tracer);
    }

    private static void assertClmAbsent(Tracer tracer) {
        assertNull(tracer.getAgentAttribute(AttributeNames.CLM_NAMESPACE));
        assertNull(tracer.getAgentAttribute(AttributeNames.CLM_FUNCTION));
    }

}
