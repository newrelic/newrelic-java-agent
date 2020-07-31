/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.servlet;

import com.newrelic.agent.ConnectionConfigListener;
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
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AttributesConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.HiddenProperties;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.ApdexStats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.util.TimeConversion;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Async timeout is given in seconds. Sleep values need to be at least as long as the value passed into createConfigMap,
 * where 0s defaults to 250ms and 1s is 1000ms.
 */
public class BasicRequestDispatcherTracerTest {

    private static final String APP_NAME = "Unit Test";

    private TransactionStats stats;

    private static Map<String, Object> createConfigMap(int timeoutInSeconds) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> web_transactions_apdex = new HashMap<>();
        web_transactions_apdex.put("WebTransaction/Custom/UrlGenerator/en/betting/Football", 7.0f);
        map.put(AgentConfigImpl.KEY_TRANSACTIONS, web_transactions_apdex);
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        map.put("apdex_t", 0.5f);
        HashMap<String, Object> attributesInclude = new HashMap<>();
        attributesInclude.put(AttributesConfigImpl.INCLUDE, "request.parameters.*");
        map.put(AgentConfigImpl.ATTRIBUTES, attributesInclude);        
        map.put("token_timeout", timeoutInSeconds);
        return map;
    }

    @Test
    public void testHttpHeaders() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader("Referer", "Referer value");
        httpRequest.setHeader("Accept", "Accept value");
        httpRequest.setHeader("Host", "Host value");
        httpRequest.setHeader("User-Agent", "User-Agent value");
        httpRequest.setHeader("Content-Length", "Content-Length value");

        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        dispatcher.getTransaction().getTransactionActivity().markAsResponseSender();
        dispatcher.getTransaction().getRootTracer().finish(0, null);

        Map<String, Object> agentAttributes = dispatcher.getTransaction().getAgentAttributes();
        assertEquals("Referer value", agentAttributes.get(AttributeNames.REQUEST_REFERER_PARAMETER_NAME));
        assertEquals("Accept value", agentAttributes.get(AttributeNames.REQUEST_ACCEPT_PARAMETER_NAME));
        assertEquals("Host value", agentAttributes.get(AttributeNames.REQUEST_HOST_PARAMETER_NAME));
        assertEquals("User-Agent value", agentAttributes.get(AttributeNames.REQUEST_USER_AGENT_PARAMETER_NAME));
        assertEquals("Content-Length value", agentAttributes.get(AttributeNames.REQUEST_CONTENT_LENGTH_PARAMETER_NAME));
    }

    @Test
    public void testHttpMethod() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setMethod("POST");

        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.getTransaction().getTransactionActivity().markAsResponseSender();
        dispatcher.getTransaction().getRootTracer().finish(0, null);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);

        Map<String, Object> agentAttributes = dispatcher.getTransaction().getAgentAttributes();
        assertEquals("POST", agentAttributes.get(AttributeNames.REQUEST_METHOD_PARAMETER_NAME));
    }

    @Before
    public void before() throws Exception {
        Map<String, Object> configMap = createConfigMap(0);
        createServiceManager(AgentConfigImpl.createAgentConfig(configMap), configMap);

        Transaction.clearTransaction();
        stats = new TransactionStats();
    }

    private static void createServiceManager(AgentConfig config, Map<String, Object> configMap) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceManager.setRPMService(rpmService);

        configService.start();

        serviceManager.setNormalizationService(new NormalizationServiceImpl());

        serviceManager.setAttributesService(new AttributesService());

        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);
        statsService.start();
    }

    private WebRequestDispatcher createDispatcher(Request httpRequest) throws Exception {
        Transaction tx = Transaction.getTransaction();
        Response httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        DefaultTracer tracer = new OtherRootTracer(tx, sig, this, new SimpleMetricNameFormat("test"));
        tx.getTransactionActivity().tracerStarted(tracer);
        WebRequestDispatcher dispatcher = new WebRequestDispatcher(httpRequest, httpResponse, tx);
        tx.setDispatcher(dispatcher);
        return dispatcher;
    }

    private void assertDelta(double expected, double actual, double tolerance) {
        Assert.assertTrue(MessageFormat.format("expected ({0}) and actual ({1}) not within tolerance({2})", expected,
                actual, tolerance), Math.abs(expected - actual) <= tolerance);
    }

    @Test
    public void noRequestXQueueHeaders() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertEquals(0, dispatcher.getQueueTime());
    }

    @Test
    public void requestXQueueStartHeader() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long queueStartTimeInMicroseconds = nowInMicroseconds - 10000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t=" + queueStartTimeInMicroseconds);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long queueStartTimeInMilliseconds = TimeUnit.MILLISECONDS.convert(queueStartTimeInMicroseconds,
                TimeUnit.MICROSECONDS);
        long expectedQueueTime = dispatcher.getTransaction().getWallClockStartTimeMs() - queueStartTimeInMilliseconds;
        assertDelta(expectedQueueTime, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXQueueStartHeaderMilliMagnitude() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMillis = Transaction.getTransaction().getWallClockStartTimeMs();
        long queueStartTimeInMillis = nowInMillis - 10;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t=" + queueStartTimeInMillis);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long expectedQueueTime = dispatcher.getTransaction().getWallClockStartTimeMs() - queueStartTimeInMillis;
        assertDelta(expectedQueueTime, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXQueueStartHeaderNanoMagnitude() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInNanos = TimeUnit.NANOSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long queueStartTimeInNanos = nowInNanos - 100000000l;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t=" + queueStartTimeInNanos);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long queueStartTimeInMilliseconds = TimeUnit.MILLISECONDS.convert(queueStartTimeInNanos, TimeUnit.NANOSECONDS);
        long expectedQueueTime = dispatcher.getTransaction().getWallClockStartTimeMs() - queueStartTimeInMilliseconds;
        assertDelta(expectedQueueTime, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXQueueStartHeaderFractionalSecondsMagnitude() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        double nowInSeconds = TimeConversion.convertMillisToSeconds(Transaction.getTransaction().getWallClockStartTimeMs());
        double queueStartTimeInSeconds = nowInSeconds - 10;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, String.format("t=%1$.3f",
                queueStartTimeInSeconds));
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        assertDelta(10000, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXQueueStartHeaderNoTEquals() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long queueStartTimeInMicroseconds = nowInMicroseconds - 10000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER,
                String.valueOf(queueStartTimeInMicroseconds));
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long queueStartTimeInMilliseconds = TimeUnit.MILLISECONDS.convert(queueStartTimeInMicroseconds,
                TimeUnit.MICROSECONDS);
        long expectedQueueTime = dispatcher.getTransaction().getWallClockStartTimeMs() - queueStartTimeInMilliseconds;
        assertDelta(expectedQueueTime, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXQueueStartHeaderTimeAfterTracerStartTime() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long dispatcherStartTimeInMicroseconds = nowInMicroseconds + 10000;
        long queueStartTimeInMicroseconds = dispatcherStartTimeInMicroseconds + 10;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t=" + queueStartTimeInMicroseconds);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertEquals(0, dispatcher.getQueueTime());
    }

    @Test
    public void requestXQueueStartHeaderNegativeValue() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long queueStartTimeInMillis = (Transaction.getTransaction().getWallClockStartTimeMs() - 100) * -1;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t=" + queueStartTimeInMillis);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertEquals(0, dispatcher.getQueueTime());
    }

    @Test
    public void requestXQueueStartHeaderNoValue() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "");
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertEquals(0, dispatcher.getQueueTime());
    }

    @Test
    public void requestXQueueStartHeaderBadValue() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t=cafebabe");
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertEquals(0, dispatcher.getQueueTime());
    }

    @Test
    public void requestXQueueStartHeaderRecordMetrics() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long queueStartTimeInMicroseconds = nowInMicroseconds - 10000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t=" + queueStartTimeInMicroseconds);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        String spec = MetricName.QUEUE_TIME.getName();
        long txStartTimeInMicroseconds = TimeUnit.MICROSECONDS.convert(
                dispatcher.getTransaction().getWallClockStartTimeMs(), TimeUnit.MILLISECONDS);
        float expectedQueueTime = (float) (txStartTimeInMicroseconds - queueStartTimeInMicroseconds)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getCallCount());
        assertDelta(expectedQueueTime, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getTotal(), .001);
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getStatsMap().size());
    }

    @Test
    public void requestXStartHeader() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long requestStartTimeInMicroseconds = nowInMicroseconds - 10000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "server1 t=" + requestStartTimeInMicroseconds);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long requestStartTimeInMilliseconds = TimeUnit.MILLISECONDS.convert(requestStartTimeInMicroseconds,
                TimeUnit.MICROSECONDS);
        long expectedExternalTime = dispatcher.getTransaction().getWallClockStartTimeMs()
                - requestStartTimeInMilliseconds;
        long actualExternalTime = dispatcher.getQueueTime();
        assertDelta(expectedExternalTime, actualExternalTime, 1);
    }

    @Test
    public void requestXStartHeaderMilliMagnitude() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMillis = Transaction.getTransaction().getWallClockStartTimeMs();
        long queueStartTimeInMillis = nowInMillis - 10;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "t=" + queueStartTimeInMillis);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long expectedQueueTime = dispatcher.getTransaction().getWallClockStartTimeMs() - queueStartTimeInMillis;
        assertDelta(expectedQueueTime, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXStartHeaderNanoMagnitude() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInNanos = TimeUnit.NANOSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long queueStartTimeInNanos = nowInNanos - 100000000l;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "t=" + queueStartTimeInNanos);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long queueStartTimeInMilliseconds = TimeUnit.MILLISECONDS.convert(queueStartTimeInNanos, TimeUnit.NANOSECONDS);
        long expectedQueueTime = dispatcher.getTransaction().getWallClockStartTimeMs() - queueStartTimeInMilliseconds;
        assertDelta(expectedQueueTime, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXStartHeaderFractionalSecondsMagnitude() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        double nowInSeconds = TimeConversion.convertMillisToSeconds(Transaction.getTransaction().getWallClockStartTimeMs());
        double queueStartTimeInSeconds = nowInSeconds - 10;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, String.format("t=%1$.3f",
                queueStartTimeInSeconds));
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        assertDelta(10000, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXStartHeaderNoTEquals() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long queueStartTimeInMicroseconds = nowInMicroseconds - 10000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, String.valueOf(queueStartTimeInMicroseconds));
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long queueStartTimeInMilliseconds = TimeUnit.MILLISECONDS.convert(queueStartTimeInMicroseconds,
                TimeUnit.MICROSECONDS);
        long expectedQueueTime = dispatcher.getTransaction().getWallClockStartTimeMs() - queueStartTimeInMilliseconds;
        assertDelta(expectedQueueTime, dispatcher.getQueueTime(), 1);
    }

    @Test
    public void requestXStartHeaderRecordMetrics() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long requestStartTimeInMicroseconds = nowInMicroseconds - 10000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "server1 t=" + requestStartTimeInMicroseconds);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long txStartTimeInMicroseconds = TimeUnit.MICROSECONDS.convert(
                dispatcher.getTransaction().getWallClockStartTimeMs(), TimeUnit.MILLISECONDS);
        float expected = (float) (txStartTimeInMicroseconds - requestStartTimeInMicroseconds)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        String spec = MetricName.QUEUE_TIME.getName();
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getCallCount());
        assertDelta(expected, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getTotal(), .001);
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getStatsMap().size());
    }

    @Test
    public void requestXStartHeaderRecordMetricsNoServerName() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long requestStartTimeInMicroseconds = nowInMicroseconds - 10000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "t=" + requestStartTimeInMicroseconds);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long txStartTimeInMicroseconds = TimeUnit.MICROSECONDS.convert(
                dispatcher.getTransaction().getWallClockStartTimeMs(), TimeUnit.MILLISECONDS);
        float expected = (float) (txStartTimeInMicroseconds - requestStartTimeInMicroseconds)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        String spec = MetricName.QUEUE_TIME.getName();
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getCallCount());
        assertDelta(expected, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getTotal(), .001);
        Assert.assertEquals(1, statsEngine.getSize());
    }

    @Test
    public void requestXStartHeaderRecordMetricsNoValue() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "");
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        Assert.assertEquals(0, statsEngine.getSize());
    }

    @Test
    public void requestXStartHeaderRecordMetricsNegativeValue() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long requestStartTimeInMillis = (Transaction.getTransaction().getWallClockStartTimeMs() - 100) * -1;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "server1 t=" + requestStartTimeInMillis);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        Assert.assertEquals(0, statsEngine.getSize());
    }

    @Test
    public void requestXStartHeaderRecordMetricsBadValue() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "server1 t=cafebabe");
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        Assert.assertEquals(0, statsEngine.getSize());
    }

    @Test
    public void requestXStartHeaderRecordMetricsMultipleServers() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long requestStartTimeInMicroseconds1 = nowInMicroseconds - 30000;
        long requestStartTimeInMicroseconds2 = nowInMicroseconds - 20000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "server1 t=" + requestStartTimeInMicroseconds1
                + "server2 t=" + requestStartTimeInMicroseconds2);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long txStartTimeInMicroseconds = TimeUnit.MICROSECONDS.convert(
                dispatcher.getTransaction().getWallClockStartTimeMs(), TimeUnit.MILLISECONDS);
        float expected1 = (float) (requestStartTimeInMicroseconds2 - requestStartTimeInMicroseconds1)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        float expected2 = (float) (txStartTimeInMicroseconds - requestStartTimeInMicroseconds2)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        float expectedTotal = (float) (txStartTimeInMicroseconds - requestStartTimeInMicroseconds1)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        String spec = MetricName.QUEUE_TIME.getName();
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getCallCount());
        assertDelta(expectedTotal, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getTotal(), .001);
        Assert.assertEquals(1, statsEngine.getSize());
    }

    @Test
    public void requestXStartHeaderRecordMetricsMultipleServersMissingServerNames() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long requestStartTimeInMicroseconds1 = nowInMicroseconds - 30000;
        long requestStartTimeInMicroseconds2 = nowInMicroseconds - 20000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "t=" + requestStartTimeInMicroseconds1 + "t="
                + requestStartTimeInMicroseconds2);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long txStartTimeInMicroseconds = TimeUnit.MICROSECONDS.convert(
                dispatcher.getTransaction().getWallClockStartTimeMs(), TimeUnit.MILLISECONDS);
        float expected1 = (float) (requestStartTimeInMicroseconds2 - requestStartTimeInMicroseconds1)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        float expected2 = (float) (txStartTimeInMicroseconds - requestStartTimeInMicroseconds2)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        float expectedTotal = (float) (txStartTimeInMicroseconds - requestStartTimeInMicroseconds1)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        String spec = MetricName.QUEUE_TIME.getName();
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getCallCount());
        assertDelta(expectedTotal, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getTotal(), .001);
        Assert.assertEquals(1, statsEngine.getSize());
    }

    @Test
    public void requestXStartHeaderRecordMetricsMultipleServersSomeMissingServerNames() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long requestStartTimeInMicroseconds1 = nowInMicroseconds - 30000;
        long requestStartTimeInMicroseconds2 = nowInMicroseconds - 20000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "t=" + requestStartTimeInMicroseconds1
                + "server2 t=" + requestStartTimeInMicroseconds2);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        long txStartTimeInMicroseconds = TimeUnit.MICROSECONDS.convert(
                dispatcher.getTransaction().getWallClockStartTimeMs(), TimeUnit.MILLISECONDS);
        float expected1 = (float) (requestStartTimeInMicroseconds2 - requestStartTimeInMicroseconds1)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        float expected2 = (float) (txStartTimeInMicroseconds - requestStartTimeInMicroseconds2)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        float expectedTotal = (float) (txStartTimeInMicroseconds - requestStartTimeInMicroseconds1)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        String spec = MetricName.QUEUE_TIME.getName();
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getCallCount());
        assertDelta(expectedTotal, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getTotal(), .001);
        Assert.assertEquals(1, statsEngine.getSize());
    }

    @Test
    public void requestXStartAndXQueueHeaderRecordMetrics() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        long nowInMicroseconds = TimeUnit.MICROSECONDS.convert(Transaction.getTransaction().getWallClockStartTimeMs(),
                TimeUnit.MILLISECONDS);
        long requestStartTimeInMicroseconds = nowInMicroseconds - 30000;
        long queueStartTimeInMicroseconds = nowInMicroseconds - 10000;
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "server1 t=" + requestStartTimeInMicroseconds);
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t=" + queueStartTimeInMicroseconds);
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        float expectedQueueTime = (float) (nowInMicroseconds - queueStartTimeInMicroseconds)
                / TimeConversion.MICROSECONDS_PER_SECOND;
        TransactionStats statsEngine = new TransactionStats();
        dispatcher.recordHeaderMetrics(statsEngine);
        String spec = MetricName.QUEUE_TIME.getName();
        Assert.assertEquals(1, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getCallCount());
        assertDelta(expectedQueueTime, statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(spec).getTotal(), .01);
        Assert.assertEquals(1, statsEngine.getSize());
    }

    @Test
    public void requestXStartHeaderRecordApdexMetrics() throws Exception {
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        rpmService.setEverConnected(true);
        Map<String, Object> data = new HashMap<>();
        data.put(AgentConfigImpl.APDEX_T, 6.0d);
        ConnectionConfigListener connectionConfigListener = rpmServiceManager.getConnectionConfigListener();
        connectionConfigListener.connected(rpmService, data);
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "server1 t="
                + (Transaction.getTransaction().getWallClockStartTimeMs() - 24005));
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.getTransaction().getRootTracer().finish(0, null);
        StatsEngine statsEngine = ServiceFactory.getStatsService().getStatsEngineForHarvest(APP_NAME);
        ApdexStats apdexStats = statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX));
        Assert.assertEquals(1, apdexStats.getApdexFrustrating());
        apdexStats = statsEngine.getApdexStats(MetricName.create("Apdex/Uri/Unknown"));
        Assert.assertEquals(1, apdexStats.getApdexFrustrating());
    }

    @Test
    public void requestXQueueHeaderRecordApdexMetrics() throws Exception {
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        rpmService.setEverConnected(true);
        Map<String, Object> data = new HashMap<>();
        data.put(AgentConfigImpl.APDEX_T, 6.0d);
        ConnectionConfigListener connectionConfigListener = rpmServiceManager.getConnectionConfigListener();
        connectionConfigListener.connected(rpmService, data);
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_START_HEADER, "server1 t="
                + (Transaction.getTransaction().getWallClockStartTimeMs() - 23500));
        httpRequest.setHeader(QueueTimeTracker.REQUEST_X_QUEUE_START_HEADER, "t="
                + (Transaction.getTransaction().getWallClockStartTimeMs() - 23500));
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.getTransaction().getRootTracer().finish(0, null);
        StatsEngine statsEngine = ServiceFactory.getStatsService().getStatsEngineForHarvest(APP_NAME);
        ApdexStats apdexStats = statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX));
        Assert.assertEquals(0, apdexStats.getApdexFrustrating());
        apdexStats = statsEngine.getApdexStats(MetricName.create("Apdex/Uri/Unknown"));
        Assert.assertEquals(0, apdexStats.getApdexFrustrating());
    }

    @Test
    public void referrerHeaderRecorded1() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader("Referer", "http://example.com:80?myparam=test&secret=donttell");
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Transaction tx = Transaction.getTransaction();
        dispatcher.setStatus(500);
        dispatcher.transactionActivityWithResponseFinished();
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertTrue("Referer header should be captured", ((String) tx.getAgentAttributes().get(
                AttributeNames.REQUEST_REFERER_PARAMETER_NAME)).contains("example.com"));
        Assert.assertFalse("Referer header shouldn't include url parameters", ((String) tx.getAgentAttributes().get(
                AttributeNames.REQUEST_REFERER_PARAMETER_NAME)).contains("donttell"));
    }

    @Test
    public void referrerHeaderRecorded2() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader("Referer", "http://example.com:80?myparam=test&secret=donttell");
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Transaction tx = Transaction.getTransaction();
        dispatcher.setStatus(500);
        dispatcher.transactionActivityWithResponseFinished();
        Assert.assertTrue("Referer header should be captured", ((String) tx.getAgentAttributes().get(
                AttributeNames.REQUEST_REFERER_PARAMETER_NAME)).contains("example.com"));
        Assert.assertFalse("Referer header shouldn't include url parameters", ((String) tx.getAgentAttributes().get(
                AttributeNames.REQUEST_REFERER_PARAMETER_NAME)).contains("donttell"));
    }

    @Test
    public void apdexFrustrating() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.setStatus(200);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertFalse(dispatcher.isApdexFrustrating());
    }

    @Test
    public void apdexFrustrating_error() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.setStatus(400);
        dispatcher.freezeStatus();
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertTrue(dispatcher.isApdexFrustrating());
    }

    @Test
    public void apdexFrustrating_ignoredError() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.setStatus(404);
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertFalse(dispatcher.isApdexFrustrating());
    }

    @Test
    public void apdexKeyTransaction() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Transaction tx = Transaction.getTransaction();
        dispatcher.setStatus(200);
        dispatcher.transactionFinished("WebTransaction/Custom/UrlGenerator/en/betting/Football", stats);
        ApdexStats stats1 = stats.getUnscopedStats().getApdexStats("Apdex/Custom/UrlGenerator/en/betting/Football");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        stats1.writeJSONString(writer);
        writer.close();
        Assert.assertEquals("[1,0,0,7.0,7.0,0]", out.toString());
    }

    @Test
    public void apdexNotKeyTransaction() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Transaction tx = Transaction.getTransaction();
        dispatcher.setStatus(200);
        dispatcher.transactionFinished("WebTransaction/Custom/UrlGenerator/ru/betting/Motorsport",
                tx.getTransactionActivity().getTransactionStats());
        ApdexStats stats1 = tx.getTransactionActivity().getTransactionStats().getUnscopedStats().getApdexStats(
                "Apdex/Custom/UrlGenerator/ru/betting/Motorsport");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        stats1.writeJSONString(writer);
        writer.close();
        Assert.assertEquals("[1,0,0,0.5,0.5,0]", out.toString());
    }

    @Test
    public void statusCodeMetric() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Transaction tx = Transaction.getTransaction();
        dispatcher.setStatus(200);
        dispatcher.freezeStatus();
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertEquals(200, dispatcher.getStatus());
        Assert.assertEquals(200, tx.getStatus());
        String expected = "Network/Inbound/StatusCode/200";
        Assert.assertEquals(1, stats.getUnscopedStats().getOrCreateResponseTimeStats(expected).getCallCount());
    }

    @Test
    public void statusCodeNoMetric() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Transaction tx = Transaction.getTransaction();
        dispatcher.transactionFinished("WebTransaction/Uri/test", stats);
        Assert.assertEquals(0, dispatcher.getStatus());
        Assert.assertEquals(0, tx.getStatus());
        String spec = "Network/Inbound/StatusCode/0";
        Assert.assertEquals(0, tx.getTransactionActivity().getTransactionStats().getUnscopedStats()
                .getOrCreateResponseTimeStats(spec).getCallCount());
    }

    @Test
    public void statusCodeLastPolicy() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.setStatus(404);
        dispatcher.setStatus(200);
        Assert.assertEquals(200, dispatcher.getStatus());
    }

    @Test
    public void statusCodeFreezePolicy() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.setStatus(404);
        dispatcher.freezeStatus();
        dispatcher.setStatus(200);
        Assert.assertEquals(404, dispatcher.getStatus());
    }

    @Test
    public void statusCodeTxaDone() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.setStatus(404);
        dispatcher.transactionActivityWithResponseFinished();
        dispatcher.setStatus(200);
        Assert.assertEquals(404, dispatcher.getStatus());
    }

    @Test
    public void statusCodePolicyProperty() throws Exception {
        ServiceManager old = ServiceFactory.getServiceManager();
        Map<String, Object> configMap = createConfigMap(0);
        configMap.put(HiddenProperties.LAST_STATUS_CODE_POLICY, false);
        createServiceManager(AgentConfigImpl.createAgentConfig(configMap), configMap);
        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        dispatcher.setStatus(404);
        dispatcher.setStatus(200);
        Assert.assertEquals(404, dispatcher.getStatus());
        ServiceFactory.getServiceManager().stop();
        ServiceFactory.setServiceManager(old);
    }

    @Test
    public void testReferrer() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader("Referer", "HelloThere!");
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Assert.assertNull(Transaction.getTransaction().getAgentAttributes().get(
                AttributeNames.REQUEST_REFERER_PARAMETER_NAME));
        dispatcher.transactionActivityWithResponseFinished();
        Assert.assertEquals("HelloThere!", Transaction.getTransaction().getAgentAttributes().get(
                AttributeNames.REQUEST_REFERER_PARAMETER_NAME));
    }

    @Test
    public void testReferrerRequestParameters() throws Exception {
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setHeader("Referer", "HelloThere;JSESSIONID=98279286928?passwd=passwd");
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Assert.assertNull(Transaction.getTransaction().getAgentAttributes().get(
                AttributeNames.REQUEST_REFERER_PARAMETER_NAME));
        dispatcher.transactionActivityWithResponseFinished();
        Assert.assertEquals("HelloThere", Transaction.getTransaction().getAgentAttributes().get(
                AttributeNames.REQUEST_REFERER_PARAMETER_NAME));
    }

    @Test
    public void testRequestParameters() throws Exception {
        MockHttpRequest httpRequest = new SpecialMockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);
        Assert.assertEquals(0, Transaction.getTransaction().getPrefixedAgentAttributes().size());
        dispatcher.transactionActivityWithResponseFinished();
        Assert.assertEquals(1, Transaction.getTransaction().getPrefixedAgentAttributes().size());
        Map params = (Map) Transaction.getTransaction().getPrefixedAgentAttributes().get("request.parameters.");
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("1", (String) params.get("one"));
        Assert.assertEquals("[2, twoo, twoooo]", ((String) params.get("two")));
    }

    private class SpecialMockHttpRequest extends MockHttpRequest {

        @Override
        public Enumeration getParameterNames() {
            return new Enumeration<String>() {
                private String[] input = new String[]{"one", "two", "three"};
                private int index = 0;

                @Override
                public boolean hasMoreElements() {
                    return index < input.length;
                }

                @Override
                public String nextElement() {
                    index++;
                    return input[index - 1];
                }
            };
        }

        @Override
        public String[] getParameterValues(String name) {
            if (name.equals("one")) {
                return new String[]{"1"};
            } else if (name.equals("two")) {
                return new String[]{"2", "twoo", "twoooo"};
            } else {
                return new String[0];
            }
        }

    }

    @Test
    public void testNanoConversionSeconds() {
        long timestamp = ExternalTimeTracker.parseTimestampToNano(TimeUnit.SECONDS.convert(
                ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO, TimeUnit.NANOSECONDS) + 1);
        assertDelta(timestamp, ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO + 1, 1000000000);
    }

    @Test(expected = NumberFormatException.class)
    public void testNanoConversionNegativeSeconds() {
        long timestamp = ExternalTimeTracker.parseTimestampToNano(-(TimeUnit.SECONDS.convert(
                ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO, TimeUnit.NANOSECONDS) + 1));
        assertDelta(timestamp, ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO + 1, 1000000000);
    }

    @Test
    public void testNanoConversionMillis() {
        long timestamp = ExternalTimeTracker.parseTimestampToNano(TimeUnit.MILLISECONDS.convert(
                ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO, TimeUnit.NANOSECONDS) + 1);
        assertDelta(timestamp, ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO + 1, 1000000);
    }

    @Test
    public void testNanoConversionMicros() {
        long timestamp = ExternalTimeTracker.parseTimestampToNano(TimeUnit.MICROSECONDS.convert(
                ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO, TimeUnit.NANOSECONDS) + 1);
        assertDelta(timestamp, ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO + 1, 10000);
    }

    @Test
    public void testNanoConversionNanos() {
        long timestamp = ExternalTimeTracker.parseTimestampToNano(ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO + 100);
        assertDelta(timestamp, ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO + 100, 1);
    }

    @Test(expected = NumberFormatException.class)
    public void testNanoConversionError() {
        ExternalTimeTracker.parseTimestampToNano(ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO);
    }

    @Test(expected = NumberFormatException.class)
    public void testNanoConversionError2() {
        ExternalTimeTracker.parseTimestampToNano(ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO - 1);
    }

    @Test(expected = NumberFormatException.class)
    public void testNanoConversionNegativeError() {
        ExternalTimeTracker.parseTimestampToNano(-(ExternalTimeTracker.EARLIEST_ACCEPTABLE_TIMESTAMP_NANO + 1));
    }

    @Test
    public void testAutoAppNamingApdexT() throws Exception {
        Map<String, Object> configMap = createConfigMap(0);

        // Configuring this in the newrelic.yml file is deprecated.
        // This is mocking a server side config property we get on connect.
        configMap.put(AgentConfigImpl.APDEX_T, 0.001f);

        createServiceManager(AgentConfigImpl.createAgentConfig(configMap), configMap);

        MockHttpRequest httpRequest = new MockHttpRequest();
        WebRequestDispatcher dispatcher = createDispatcher(httpRequest);

        // App name is something other than APP_NAME = "Unit Test"
        dispatcher.getTransaction().setApplicationName(ApplicationNamePriority.SERVLET_INIT_PARAM, "SecondAppName");
        dispatcher.transactionFinished("WebTransaction/transactionName", new TransactionStats());
    }
}
