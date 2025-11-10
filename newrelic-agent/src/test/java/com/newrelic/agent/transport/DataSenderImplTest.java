/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.ForceDisconnectException;
import com.newrelic.agent.ForceRestartException;
import com.newrelic.agent.LicenseException;
import com.newrelic.agent.MaxPayloadException;
import com.newrelic.agent.MetricData;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.PathHashes;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventFactory;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventBuilder;
import com.newrelic.agent.stats.IncrementCounter;
import com.newrelic.agent.stats.RecordDataUsageMetric;
import com.newrelic.agent.stats.StatsImpl;
import com.newrelic.agent.stats.StatsService;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;
import org.mockito.invocation.Invocation;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.newrelic.agent.MetricNames.SUPPORTABILITY_AGENT_ENDPOINT_HTTP_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class DataSenderImplTest {

    private static final String SUPPORTABILITY_METRIC_METRIC_DATA = "Supportability/Agent/Collector/MaxPayloadSizeLimit/metric_data";
    private static final String SUPPORTABILITY_METRIC_ANALYTIC_DATA = "Supportability/Agent/Collector/MaxPayloadSizeLimit/analytic_event_data";
    private static final String SUPPORTABILITY_METRIC_SPAN_DATA = "Supportability/Agent/Collector/MaxPayloadSizeLimit/span_event_data";
    private static final String MAX_PAYLOAD_EXCEPTION = MaxPayloadException.class.getSimpleName();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Mock
    public StatsService mockStatsService;

    @Mock
    public AttributesService mockAttributesService;

    @Mock
    public IAgentLogger logger;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        final MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setStatsService(mockStatsService);
        serviceManager.setAttributesService(mockAttributesService);

        when(mockAttributesService.filterAttributes(anyString(), ArgumentMatchers.<String, Object>anyMap()))
                .thenAnswer(new ReturnsArgumentAt(1));
    }

    @After
    public void tearDown() {
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void testStatusCodeProxyAuthenticate() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperReturning407 = getProxyAuthenticateFailingWrapper(null);

        DataSenderImpl target = new DataSenderImpl(config, wrapperReturning407, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(HttpError.class);
        exceptionRule.expectMessage("Proxy Authentication Mechanism Failed: null Proxy-Authenticate");
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testStatusCodeProxyAuthenticateWithHeader() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperReturning407 = getProxyAuthenticateFailingWrapper("challenge");

        DataSenderImpl target = new DataSenderImpl(config, wrapperReturning407, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(HttpError.class);
        exceptionRule.expectMessage("Proxy Authentication Mechanism Failed: challenge");
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testUnauthorizedFailureNoException() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperReturning401 = getHttpClientWrapper(ReadResult.create(HttpResponseCode.UNAUTHORIZED, "something not json", null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperReturning401, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(LicenseException.class);
        exceptionRule.expectMessage("something not json");
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testUnauthorizedFailureWithException() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperReturning401 = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.UNAUTHORIZED,
                "{\"exception\":{\"message\":\"double escaping to prove json parsing: \\\" <- that thing\"}}",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperReturning401, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(LicenseException.class);
        exceptionRule.expectMessage("double escaping to prove json parsing: \" <- that thing");
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testConflictThrowsForceRestart() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperReturning409 = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.CONFLICT,
                "force restart!",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperReturning409, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(ForceRestartException.class);
        exceptionRule.expectMessage("force restart!");
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testGoneThrowsForceDisconnect() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperReturning410 = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.GONE,
                "all gone!",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperReturning410, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(ForceDisconnectException.class);
        exceptionRule.expectMessage("all gone!");
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testOtherErrorStatusThrowsHttpError() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperReturning410 = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.INTERNAL_SERVER_ERROR,
                "big oops!",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperReturning410, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        // the response body is logged, but otherwise ignored in these cases.
        exceptionRule.expect(HttpError.class);
        exceptionRule.expectMessage("encountered an internal error");
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testSuccessEmptyResponseListenerSideEffects() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperEmptyReturn = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.ACCEPTED,
                "",
                null));

        final AtomicBoolean dataReceivedCalled = new AtomicBoolean(false);
        final AtomicBoolean dataSentCalled = new AtomicBoolean(false);
        DataSenderImpl target = new DataSenderImpl(config, wrapperEmptyReturn, new DataSenderListener() {
            @Override
            public void dataSent(String method, String encoding, String uri, byte[] rawDataSent) {
                dataSentCalled.set(true);
            }

            @Override
            public void dataReceived(String method, String encoding, String uri, Map<?, ?> rawDataReceived) {
                dataReceivedCalled.set(true);
            }
        }, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
        assertFalse("Did not expect data received to be called with no body", dataReceivedCalled.get());
        assertTrue("expected dataSent to be called!", dataSentCalled.get());
    }

    @Test
    public void testDataUsageSupportability() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperEmptyReturn = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.OK,
                "",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperEmptyReturn, null, logger, ServiceFactory.getConfigService());

        target.setAgentRunId("agent run id");

        List<MetricData> metricData = createMetricData(5);
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(), metricData);

        // Expected payload sent after adding 5 metrics and agent metadata
        final String expectedSentPayload = "[\"agent run id\",1644424673,1644424678," +
                "[[0,[1,1.0,1.0,1.0,1.0,1.0]]," +
                "[1,[1,1.0,1.0,1.0,1.0,1.0]]," +
                "[2,[1,1.0,1.0,1.0,1.0,1.0]]," +
                "[3,[1,1.0,1.0,1.0,1.0,1.0]]," +
                "[4,[1,1.0,1.0,1.0,1.0,1.0]]]]";
        int expectedSentPayloadSizeInBytes = expectedSentPayload.getBytes().length;

        // Expected payload received is empty
        final String expectedReceivedPayload = "";
        int expectedReceivedPayloadSizeInBytes = expectedReceivedPayload.getBytes().length;

        String collectorOutputBytesMetric = MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES, "Collector");
        String collectorEndpointOutputBytesMetric = MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES, "Collector", "metric_data");

        assertMetricWasRecorded(MessageFormat.format(MetricNames.SUPPORTABILITY_HTTP_CODE, HttpResponseCode.OK));
        assertMetricWasRecorded(collectorOutputBytesMetric);
        assertMetricWasRecorded(collectorEndpointOutputBytesMetric);

        assertDataUsageMetricValues(collectorOutputBytesMetric, expectedSentPayloadSizeInBytes, expectedReceivedPayloadSizeInBytes);
        assertDataUsageMetricValues(collectorEndpointOutputBytesMetric, expectedSentPayloadSizeInBytes, expectedReceivedPayloadSizeInBytes);
    }

    @Test
    public void testSuccessSupportability() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperEmptyReturn = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.OK,
                "",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperEmptyReturn, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));

        assertMetricWasRecorded(MessageFormat.format(MetricNames.SUPPORTABILITY_HTTP_CODE, HttpResponseCode.OK));
    }

    @Test
    public void testSupportabilityMetricOnException() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperReturning410 = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.INTERNAL_SERVER_ERROR,
                "big oops!",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperReturning410, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        try {
            target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                    createMetricData(5));
            // verify that we got an exception
            fail("Should not have gotten here");
        } catch (HttpError ignored) {
            assertMetricWasRecorded(MessageFormat.format(SUPPORTABILITY_AGENT_ENDPOINT_HTTP_ERROR, HttpResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void testMalformedURLMappedToForceDisconnect() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        Exception toThrow = null;
        try {
            new URL("......").toURI();
        } catch (Exception e) {
            toThrow = e;
        }

        assertNotNull(toThrow);
        HttpClientWrapper wrapper = getThrowingClientWrapper(toThrow);
        DataSenderImpl target = new DataSenderImpl(config, wrapper, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(ForceDisconnectException.class);
        exceptionRule.expectMessage(toThrow.getClass().getName());
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testSocketExceptionRethrown() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapper = getThrowingClientWrapper(new SocketException());
        DataSenderImpl target = new DataSenderImpl(config, wrapper, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(SocketException.class);
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testSocketExceptionWithNoSuchAlgorithmRethrown() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        SocketException toThrow = new SocketException();
        toThrow.initCause(new NoSuchAlgorithmException());
        HttpClientWrapper wrapper = getThrowingClientWrapper(toThrow);
        DataSenderImpl target = new DataSenderImpl(config, wrapper, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(SocketException.class);
        exceptionRule.expectCause(CoreMatchers.<Throwable>instanceOf(NoSuchAlgorithmException.class));
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testSSLHandshakeRethrown() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        SSLHandshakeException toThrow = new SSLHandshakeException("whatever");
        HttpClientWrapper wrapper = getThrowingClientWrapper(toThrow);
        DataSenderImpl target = new DataSenderImpl(config, wrapper, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(SSLHandshakeException.class);
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testOtherExceptionRethrown() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        Exception toThrow = new NullPointerException("everyone's favorite");
        HttpClientWrapper wrapper = getThrowingClientWrapper(toThrow);
        DataSenderImpl target = new DataSenderImpl(config, wrapper, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(NullPointerException.class);
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testThrowsOnUnparseablePayload() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperEmptyReturn = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.OK,
                "{{{{",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperEmptyReturn, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(CoreMatchers.anything());
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testThrowsOnParseableNonObjectPayload() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperEmptyReturn = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.OK,
                "[1,2,3,4]",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperEmptyReturn, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        exceptionRule.expect(ClassCastException.class);
        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));
    }

    @Test
    public void testNoExceptionBodyParseableButMissingReturnValue() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperEmptyReturn = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.OK,
                "{\"something\":true}",
                null));

        final AtomicBoolean dataReceivedCalled = new AtomicBoolean(false);
        final AtomicBoolean dataSentCalled = new AtomicBoolean(false);
        DataSenderImpl target = new DataSenderImpl(config, wrapperEmptyReturn, new DataSenderListener() {
            @Override
            public void dataSent(String method, String encoding, String uri, byte[] rawDataSent) {
                dataSentCalled.set(true);
            }

            @Override
            public void dataReceived(String method, String encoding, String uri, Map<?, ?> rawDataReceived) {
                dataReceivedCalled.set(true);
            }
        }, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        target.sendMetricData(System.currentTimeMillis() - 5000, System.currentTimeMillis(),
                createMetricData(5));

        assertTrue("expected listener data received", dataReceivedCalled.get());
    }

    @Test
    public void testReturnsParsedAgentCommands() throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        HttpClientWrapper wrapperEmptyReturn = getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.OK,
                "{\"return_value\":[[3,4,5]]}",
                null));

        DataSenderImpl target = new DataSenderImpl(config, wrapperEmptyReturn, null, logger, ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        List<List<?>> result = target.getAgentCommands();
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(4L, result.get(0).get(1));
    }

    /**
     * Verify that a given metric was created
     *
     * @param expectedMetricName name of metric to verify
     */
    private void assertMetricWasRecorded(String expectedMetricName) {
        boolean found = false;
        MockingDetails output = Mockito.mockingDetails(mockStatsService);
        for (Invocation invocation: output.getInvocations()) {
            if (found) {
                break;
            }
            String methodName = invocation.getMethod().getName();
            if (invocation.getRawArguments() != null && invocation.getRawArguments().length > 0) {
                Object rawArgument = invocation.getRawArguments()[0];
                if (rawArgument instanceof IncrementCounter) {
                    String metricName = invocation.<IncrementCounter>getArgument(0).getName();
                    found = methodName.equals("doStatsWork") && metricName.equals(expectedMetricName);
                } else if (rawArgument instanceof RecordDataUsageMetric) {
                    String metricName = invocation.<RecordDataUsageMetric>getArgument(0).getName();
                    found = methodName.equals("doStatsWork") && metricName.equals(expectedMetricName);
                }
            }
        }
        assertTrue("Could not find metric: " + expectedMetricName, found);
    }

    /**
     * Verify the sent/received payload sizes recorded by a given RecordDataUsageMetric
     *
     * @param expectedMetricName name of metric to verify
     * @param expectedBytesSent expected size of sent payload in bytes
     * @param expectedBytesReceived expected size of received payload in bytes
     */
    private void assertDataUsageMetricValues(String expectedMetricName, int expectedBytesSent, int expectedBytesReceived) {
        boolean found = false;
        MockingDetails output = Mockito.mockingDetails(mockStatsService);
        for (Invocation invocation: output.getInvocations()) {
            String methodName = invocation.getMethod().getName();
            Object rawArgument = invocation.getRawArguments()[0];
            if (rawArgument instanceof RecordDataUsageMetric) {
                String metricName = invocation.<RecordDataUsageMetric>getArgument(0).getName();
                found = methodName.equals("doStatsWork") && metricName.equals(expectedMetricName);
                if (found) {
                    assertEquals(expectedBytesSent, ((RecordDataUsageMetric) rawArgument).getBytesSent());
                    assertEquals(expectedBytesReceived, ((RecordDataUsageMetric) rawArgument).getBytesReceived());
                    break;
                }
            }
        }
        assertTrue("Could not find metric: " + expectedMetricName, found);
    }

    @Test
    public void testMaxPayloadSize() {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap());
        DataSenderImpl dataSender = new DataSenderImpl(config, getHttpClientWrapper(), null, logger, ServiceFactory.getConfigService());

        dataSender.setAgentRunId("AgentRunId");
        dataSender.setMaxPayloadSizeInBytes(200);

        sendAnalyticEventsPayloadTooBig(dataSender);
        sendMetricDataPayloadTooBig(dataSender);
        sendSpanEventsPayloadTooBig(dataSender);
        sendLogEventsPayloadTooBig(dataSender);

        sendMetricDataSmallPayload(dataSender);

        assertMetricWasRecorded(SUPPORTABILITY_METRIC_METRIC_DATA);
        assertMetricWasRecorded(SUPPORTABILITY_METRIC_ANALYTIC_DATA);
        assertMetricWasRecorded(SUPPORTABILITY_METRIC_SPAN_DATA);
    }

    private HttpClientWrapper getProxyAuthenticateFailingWrapper(String proxyAuthenticateHeader) {
        return getHttpClientWrapper(ReadResult.create(
                HttpResponseCode.PROXY_AUTHENTICATION_REQUIRED,
                null,
                proxyAuthenticateHeader
        ));
    }

    private HttpClientWrapper getHttpClientWrapper() {
        return getHttpClientWrapper(null);
    }

    private <T extends Exception> HttpClientWrapper getThrowingClientWrapper(final T exception) {
        return new HttpClientWrapper() {
            @Override
            public ReadResult execute(Request request, ExecuteEventHandler eventHandler) throws Exception {
                throw exception;
            }

            @Override
            public void captureSupportabilityMetrics(StatsService statsService, String requestHost) {
            }

            @Override
            public void shutdown() {
            }
        };
    }

    private HttpClientWrapper getHttpClientWrapper(final ReadResult readResult) {
        return new HttpClientWrapper() {
            @Override
            public ReadResult execute(Request request, ExecuteEventHandler eventHandler) {
                return readResult;
            }

            @Override
            public void captureSupportabilityMetrics(StatsService statsService, String requestHost) {
            }

            @Override
            public void shutdown() {
            }
        };
    }

    private void sendMetricDataSmallPayload(DataSenderImpl dataSender) {
        try {
            // ~ 48 bytes
            dataSender.sendMetricData(System.currentTimeMillis() - 60, System.currentTimeMillis(), createMetricData(1));
        } catch (MaxPayloadException e) {
            Assert.fail("Did not fail the way we expected");
        } catch (Exception e) {
            // all fine
        }
    }

    private void sendLogEventsPayloadTooBig(DataSenderImpl dataSender) {
        boolean exceptionThrown = false;
        try {
            dataSender.sendLogEvents(createLogEvents(10000));
        } catch (Exception e) {
            assertEquals(MAX_PAYLOAD_EXCEPTION, e.getClass().getSimpleName());
            exceptionThrown = true;
        }
        assertTrue("MaxPayloadException was NOT thrown as expected", exceptionThrown);
    }

    private void sendAnalyticEventsPayloadTooBig(DataSenderImpl dataSender) {
        try {
            // ~ 943 bytes
            dataSender.sendAnalyticsEvents(10000, 10000, createTransactionEvents(1000));
        } catch (Exception e) {
            assertEquals(MAX_PAYLOAD_EXCEPTION, e.getClass().getSimpleName());
        }
    }

    private void sendMetricDataPayloadTooBig(DataSenderImpl dataSender) {
        try {
            // ~ 2378 bytes
            dataSender.sendMetricData(System.currentTimeMillis() - 60, System.currentTimeMillis(), createMetricData(1000));
        } catch (Exception e) {
            assertEquals(MAX_PAYLOAD_EXCEPTION, e.getClass().getSimpleName());
        }
    }

    private void sendSpanEventsPayloadTooBig(DataSenderImpl dataSender) {
        try {
            // ~ 999 bytes
            dataSender.sendSpanEvents(10000, 10000, createSpanEvents(1000));
        } catch (Exception e) {
            assertEquals(MAX_PAYLOAD_EXCEPTION, e.getClass().getSimpleName());
        }
    }

    private List<SpanEvent> createSpanEvents(int i) {
        List<SpanEvent> events = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            events.add(new SpanEventFactory("Test")
                    .setCategory(SpanCategory.datastore)
                    .setDurationInSeconds(1234)
                    .build());

        }
        return events;
    }

    private List<TransactionEvent> createTransactionEvents(int size) {
        List<TransactionEvent> events = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            events.add(new TransactionEventBuilder()
                    .setAppName("App Name")
                    .setPathHashes(new PathHashes(null, null, "pathhashes"))
                    .setDuration(1234)
                    .setError(false)
                    .setName("Name")
                    .setPriority(0.22f)
                    .setGuid("guid")
                    .setPort(8080)
                    .setTripId("tripId")
                    .build());
        }
        return events;
    }

    private List<LogEvent> createLogEvents(int size) {
        List<LogEvent> events = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("key", "value");
            events.add(new LogEvent(attrs, 0));
        }
        return events;
    }

    private List<MetricData> createMetricData(int metrics) {
        List<MetricData> metricData = new ArrayList<>();
        for (int i = 0; i < metrics; i++) {
            metricData.add(MetricData.create(MetricName.create(String.valueOf(i)), i, new StatsImpl(1, 1, 1, 1, 1)));
        }
        return metricData;
    }

    public Map<String, Object> configMap() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("host", "localhost");
        configMap.put("port", 9999);
        return configMap;
    }

}