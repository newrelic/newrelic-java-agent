/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.api.agent.GenericParameters;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.net.URI;

import static com.newrelic.agent.AgentHelper.getFullPath;

public class ExternalTracerTest {

    @After
    public void after() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    public void before(String ymlPath) throws Exception {
        String configPath = getFullPath(ymlPath);
        System.setProperty("newrelic.config.file", configPath);
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        String appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(appName);
        eventPool.clear();
    }

    @Test
    public void testExternalTracerIncludeUri() throws Exception {
        before("/com/newrelic/agent/config/span_events.yml");
        DefaultTracer tracer = prepareTracer();
        TransactionTracerConfig ttConfig = ServiceFactory.getConfigService()
                .getDefaultAgentConfig()
                .getTransactionTracerConfig();
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionSegment segment = tracer.getTransactionSegment(ttConfig,
                sqlObfuscator, 0, null);
        TransactionStats stats = tracer.getTransactionActivity().getTransactionStats();
        String uri = "http://myhost:1234/Parameters";
        String queryParams = "?data=confidential";
        tracer.reportAsExternal(GenericParameters
                .library("MyLibrary")
                .uri(URI.create(uri + queryParams))
                .procedure("other")
                .build());
        tracer.recordMetrics(stats);
        String segmentUri = segment.getUri();
        tracer.finish(Opcodes.RETURN, null);
        Assert.assertNotNull(segmentUri);
    }

    @Test
    public void testExternalTracerExcludeUri() throws Exception {
        before("/com/newrelic/agent/config/exclude_request_uri.yml");

        DefaultTracer tracer = prepareTracer();
        TransactionTracerConfig ttConfig = ServiceFactory.getConfigService()
                .getDefaultAgentConfig()
                .getTransactionTracerConfig();
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionSegment segment = tracer.getTransactionSegment(ttConfig,
                sqlObfuscator, 0, null);
        TransactionStats stats = tracer.getTransactionActivity().getTransactionStats();
        String uri = "http://myhost:1234/Parameters";
        String queryParams = "?data=confidential";
        tracer.reportAsExternal(GenericParameters
                .library("MyLibrary")
                .uri(URI.create(uri + queryParams))
                .procedure("other")
                .build());
        tracer.recordMetrics(stats);
        String segmentUri = segment.getUri();
        tracer.finish(Opcodes.RETURN, null);
        Assert.assertEquals(null, segmentUri);
    }

    public DefaultTracer prepareTracer() {
        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();

        DefaultTracer tracer = new OtherRootTracer(tx, new ClassMethodSignature("class",
                "method", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        tx.getTransactionActivity().tracerStarted(tracer);

        return tracer;
    }
}
