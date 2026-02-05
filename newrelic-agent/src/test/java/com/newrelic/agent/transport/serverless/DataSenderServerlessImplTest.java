/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MetricData;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.ServerlessConfig;
import com.newrelic.agent.config.SqlTraceConfig;
import com.newrelic.agent.errors.HttpTracedError;
import com.newrelic.agent.errors.ThrowableError;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.model.ApdexPerfZone;
import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.CountedDuration;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.EventOnSpan;
import com.newrelic.agent.model.LinkOnSpan;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.model.SyntheticsIds;
import com.newrelic.agent.model.SyntheticsInfo;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventFactory;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventBuilder;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.sql.DataSenderServerlessSqlUtil;
import com.newrelic.agent.sql.SlowQueryInfo;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsImpl;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataSenderServerlessImplTest {
    private ServerlessWriter serverlessWriter;

    private DataSenderServerlessImpl dataSender = null;

    private static final String APP_NAME = "Unit Test";
    private static final Pattern FILE_PAYLOAD_PATTERN = Pattern.compile(
            "\\[2,\"NR_LAMBDA_MONITORING\",\\{\"agent_version\":\"9\\.0\\.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?\"]");
    private static final Pattern SQL_CONSOLE_PAYLOAD_PATTERN = Pattern.compile(
            "\\[2,\"NR_LAMBDA_MONITORING\",\\{\"agent_version\":\"9\\.0\\.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\\{\"sql_trace_data\":\\[\\[\\[null,\"http://jvm\\.agent\\.uri\",-764488978,\"select \\? from \\?\",null,0,0,9223372036854,0,\"[^\"]*\"]]]}]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRACED_ERROR_CONSOLE_PAYLOAD_PATTERN = Pattern.compile(
            "\\[2,\"NR_LAMBDA_MONITORING\",\\{\"agent_version\":\"9\\.0\\.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\\{\"error_data\":\\[null,\\[\\[10,\"dude\",\"HttpClientError 403\",\"HttpClientError 403\",\\{\"userAttributes\":\\{\"uAttr\":\"uVal\"},\"intrinsics\":\\{\"iAttr\":\"iVal\",\"error\\.expected\":false},\"stack_traces\":\\{\\},\"agentAttributes\":\\{\"aAttr\":\"aVal\",\"request\\.uri\":\"/dude\"}}],\\[10,\"metric\",\"\",\"java\\.lang\\.RuntimeException\",\\{\"userAttributes\":\\{\"uAttr\":\"uVal\"\\},\"intrinsics\":\\{\"iAttr\":\"iVal\",\"error\\.expected\":false},\"agentAttributes\":\\{\"aAttr\":\"aVal\",\"request\\.uri\":\"\"},\"stack_trace\":\\[[^]]*]}]]]}]", Pattern.CASE_INSENSITIVE);
    private static final ArgumentMatcher<String> FILE_PAYLOAD_MATCHER = actual -> {
        Matcher matcher = FILE_PAYLOAD_PATTERN.matcher(actual);
        return matcher.matches();
    };

    @Mock
    public IAgentLogger logger;

    @Mock
    public ServerlessConfig serverlessConfig;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        HashMap<String, Object> props = new HashMap<>();
        setupServiceManager(props);

        serverlessWriter = Mockito.mock(ServerlessWriter.class);

        Mockito.when(serverlessConfig.getArn()).thenReturn("TMP_ARN");
        Mockito.when(serverlessConfig.getFunctionVersion()).thenReturn("15");

        DataSenderServerlessConfig config = new DataSenderServerlessConfig("9.0.0", serverlessConfig);
        this.dataSender = new DataSenderServerlessImpl(config, logger, serverlessWriter);
    }

    @Test
    public void testTracedErrors() throws Exception {
        Map<String, Object> intrinsicAttributes = new HashMap<>();
        intrinsicAttributes.put("iAttr", "iVal");

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");
        Map<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put("aAttr", "aVal");

        TracedError httpTracedError = HttpTracedError
                .builder(ServiceFactory.getConfigService().getErrorCollectorConfig(APP_NAME), APP_NAME, "dude",
                        10L)
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .intrinsicAttributes(intrinsicAttributes)
                .userAttributes(userAttributes)
                .agentAttributes(agentAttributes)
                .build();
        TracedError throwableError = ThrowableError.builder(ServiceFactory.getConfigService().getErrorCollectorConfig(APP_NAME), APP_NAME,
                "metric", new RuntimeException(),
                10L)
                .intrinsicAttributes(intrinsicAttributes)
                .agentAttributes(agentAttributes)
                .userAttributes(userAttributes)
                .build();
        dataSender.sendErrorData(Arrays.asList(httpTracedError, throwableError));

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();
        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.argThat(actual -> TRACED_ERROR_CONSOLE_PAYLOAD_PATTERN.matcher(actual).matches()));
    }

    @Test
    public void testErrorEvents() throws Exception {
        Collection<ErrorEvent> errorEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");
        Map<String, String> syntheticsAttr = new HashMap<>();
        syntheticsAttr.put("sAttr", "sVal");
        Map<String, Object> dTIntrinsics = new HashMap<>();
        dTIntrinsics.put("dAttr", "dVal");
        Map<String, Object> agentAttr = new HashMap<>();
        agentAttr.put("aAttr", "aVal");

        ErrorEvent errorEvent = new ErrorEvent("appName", 10L, 1.05f, userAttributes, "MyClass", "message",
                true, "txnName",
                111, 2, 3, 4, 5, 6, 7,
                "txnGuid", "refTxnGuid", "syntheticsResourceId",
                "sm", "sj", "st", "si", syntheticsAttr, 8080,
                "cause", "tripId", dTIntrinsics, agentAttr, new AttributeFilter.PassEverythingAttributeFilter());

        errorEvents.add(errorEvent);
        dataSender.sendErrorEvents(111, 1, errorEvents);

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.eq("[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"error_event_data\":[null,{\"events_seen\":1,\"reservoir_size\":111},[[{\"type\":\"TransactionError\",\"transactionName\":\"txnName\",\"nr.syntheticsMonitorId\":\"sm\",\"nr.syntheticsType\":\"st\",\"duration\":111.0,\"nr.transactionGuid\":\"txnGuid\",\"dAttr\":\"dVal\",\"nr.referringTransactionGuid\":\"refTxnGuid\",\"databaseCallCount\":6.0,\"nr.syntheticsSAttr\":\"sVal\",\"timestamp\":10,\"error.expected\":true,\"nr.timeoutCause\":\"cause\",\"error.class\":\"MyClass\",\"databaseDuration\":4.0,\"nr.syntheticsJobId\":\"sj\",\"externalCallCount\":7.0,\"priority\":1.05,\"nr.syntheticsInitiator\":\"si\",\"gcCumulative\":5.0,\"queueDuration\":2.0,\"port\":8080,\"error.message\":\"message\",\"externalDuration\":3.0,\"guid\":\"txnGuid\",\"nr.syntheticsResourceId\":\"syntheticsResourceId\",\"nr.tripId\":\"tripId\"},{\"uAttr\":\"uVal\"},{\"aAttr\":\"aVal\"}]]]}]")
        );
    }

    @Test
    public void testSpanEvents() throws Exception {
        Collection<SpanEvent> spanEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");
        SpanEventFactory spanEventFactory = new SpanEventFactory("myAppName");
        SpanEvent spanEvent = spanEventFactory
                .setPriority(1.0f)
                .putAgentAttribute("aAttr", "aVal")
                .setKind("producer")
                .putIntrinsicAttribute("iAttr", "iVal")
                .putAllUserAttributes(userAttributes)
                .setTimestamp(11232344)
                .build();

        spanEvents.add(spanEvent);
        dataSender.sendSpanEvents(111, 1, spanEvents);

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.eq("[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"span_event_data\":[null,{\"events_seen\":1,\"reservoir_size\":111},[[{\"span.kind\":\"producer\",\"type\":\"Span\",\"category\":\"generic\",\"priority\":1.0,\"iAttr\":\"iVal\",\"timestamp\":11232344},{\"uAttr\":\"uVal\"},{\"aAttr\":\"aVal\"}]]]}]")
        );
    }

    @Test
    public void testOTelSpanEvents() throws Exception {
        Collection<SpanEvent> spanEvents = new ArrayList<>();
        SpanEventFactory spanEventFactory = new SpanEventFactory("myAppName");
        SpanEvent spanEvent = spanEventFactory
                .setPriority(1.0f)
                .putAgentAttribute("aAttr", "aVal")
                .setKind("producer")
                .putIntrinsicAttribute("iAttr", "iVal")
                .putAllUserAttributes(Collections.singletonMap("uAttr", "uVal"))
                .setTimestamp(11232344)
                .setTraceId("123")
                .setLinkOnSpanEvents(Arrays.asList(
                        LinkOnSpan.builder()
                                .appName(APP_NAME)
                                .priority(23234)
                                .putAllUserAttributes(Collections.singletonMap("uAttr", "uVal2"))
                                .putIntrinsic("trace.id", "123")
                                .putIntrinsic("type", "SpanLink")
                                .putIntrinsic("id", "1234")
                                .putIntrinsic("linkedTraceId", "123244")
                                .putIntrinsic("timestamp", 234234234L)
                                .putAgentAttribute("aAttr", "aVal2")
                        .build(),
                        LinkOnSpan.builder()
                                .appName(APP_NAME)
                                .priority(23234)
                                .putAllUserAttributes(Collections.singletonMap("uAttr", "uVal3"))
                                .putIntrinsic("trace.id", "123")
                                .putIntrinsic("type", "SpanLink")
                                .putIntrinsic("id", "1234")
                                .putIntrinsic("linkedTraceId", "354674586754")
                                .putIntrinsic("timestamp", 234234234L)
                                .putIntrinsic("linkedSpanId", "1234")
                                .putAgentAttribute("aAttr", "aVal3")
                                .build()))
                .setEventOnSpanEvents(Arrays.asList(
                        EventOnSpan.builder()
                                .appName(APP_NAME)
                                .priority(23234)
                                .putIntrinsic("trace.id", "123")
                                .putIntrinsic("type", "SpanEvent")
                                .putIntrinsic("span.id", "1234")
                                .putIntrinsic("name", "MyEvent1")
                                .putIntrinsic("timestamp", 234234234L)
                                .putAllUserAttributes(Collections.singletonMap("uAttr", "uVal4"))
                                .putAgentAttribute("aAttr", "aVal4")
                                .build(),
                        EventOnSpan.builder()
                                .appName(APP_NAME)
                                .priority(23234)
                                .putAllUserAttributes(Collections.singletonMap("uAttr", "uVal5"))
                                .putIntrinsic("trace.id", "123")
                                .putIntrinsic("type", "SpanEvent")
                                .putIntrinsic("span.id", "1234")
                                .putIntrinsic("name", "MyEvent1")
                                .putIntrinsic("timestamp", 234234234L)
                                .putAgentAttribute("aAttr", "aVal5")
                                .build()
                ))
                .build();

        spanEvents.add(spanEvent);
        dataSender.sendSpanEvents(111, 1, spanEvents);

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.eq("[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"span_event_data\":[null,{\"events_seen\":1,\"reservoir_size\":111},[[{\"traceId\":\"123\",\"span.kind\":\"producer\",\"type\":\"Span\",\"category\":\"generic\",\"priority\":1.0,\"iAttr\":\"iVal\",\"timestamp\":11232344},{\"uAttr\":\"uVal\"},{\"aAttr\":\"aVal\"}],[{\"trace.id\":\"123\",\"id\":\"1234\",\"type\":\"SpanLink\",\"linkedTraceId\":\"123244\",\"timestamp\":234234234},{\"uAttr\":\"uVal2\"},{\"aAttr\":\"aVal2\"}],[{\"trace.id\":\"123\",\"linkedSpanId\":\"1234\",\"id\":\"1234\",\"type\":\"SpanLink\",\"linkedTraceId\":\"354674586754\",\"timestamp\":234234234},{\"uAttr\":\"uVal3\"},{\"aAttr\":\"aVal3\"}],[{\"trace.id\":\"123\",\"name\":\"MyEvent1\",\"span.id\":\"1234\",\"type\":\"SpanEvent\",\"timestamp\":234234234},{\"uAttr\":\"uVal4\"},{\"aAttr\":\"aVal4\"}],[{\"trace.id\":\"123\",\"name\":\"MyEvent1\",\"span.id\":\"1234\",\"type\":\"SpanEvent\",\"timestamp\":234234234},{\"uAttr\":\"uVal5\"},{\"aAttr\":\"aVal5\"}]]]}]")
        );
    }

    @Test
    public void testAnalyticEvents() throws Exception {
        sendTransactionEvent();
        sendCustomEvent();
        sendLogEvent();

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.eq("[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"analytic_event_data\":[null,{\"events_seen\":3,\"reservoir_size\":136},[[{\"type\":\"Transaction\",\"error\":false,\"nr.syntheticsMonitorId\":\"b\",\"nr.syntheticsType\":\"myType\",\"duration\":10000.0,\"dAttr\":\"dVal\",\"apdexPerfZone\":\"S\",\"databaseCallCount\":3.0,\"nr.syntheticsSAttr\":\"sVal\",\"timestamp\":1414523453253251212,\"nr.timeoutCause\":\"segment\",\"totalTime\":1001.0,\"nr.syntheticsJobId\":\"c\",\"databaseDuration\":1.0,\"externalCallCount\":5.0,\"priority\":0.03,\"timeToFirstByte\":1.0,\"nr.syntheticsInitiator\":\"someVal\",\"timeToLastByte\":2.0,\"gcCumulative\":1100.0,\"port\":8081,\"queueDuration\":23.0,\"externalDuration\":2.0,\"name\":\"txnName\",\"nr.syntheticsResourceId\":\"a\"},{\"uAttr\":\"uVal\"},{\"aAttr\":\"aVal\"}],[{\"type\":\"Custom\"},{\"uAttr\":\"uVal\"},{}],[{\"type\":\"LogEvent\"},{\"attr\":\"val\"},{}]]]}]")
        );

        Mockito.clearInvocations(serverlessWriter);
        // Verify the buffer actually cleared
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.eq("[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{}]")
        );
    }

    private void sendTransactionEvent() throws Exception {
        Collection<TransactionEvent> transactionEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");

        Map<String, String> syntheticsAttr = new HashMap<>();
        syntheticsAttr.put("sAttr", "sVal");

        Map<String, Object> dtIntrinsics = new HashMap<>();
        dtIntrinsics.put("dAttr", "dVal");

        TransactionEvent transactionEvent = new TransactionEventBuilder()
                .setAppName("appName")
                .setTimestamp(1414523453253251212L)
                .setName("txnName")
                .setDuration(10000)
                .setGuid("asdad")
                .setReferringGuid("ewqadads")
                .setPort(8081)
                .setTripId("adadadsa")
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(1001)
                .setTimeoutCause(null)
                .setPriority(.25F)
                .putAllUserAttributes(userAttributes)
                .setDatabase(new CountedDuration(1, 3))
                .setExternal(new CountedDuration(2, 5))
                .setQueueDuration(23)
                .setGcCumulative(1100)
                .setTimeToFirstByte(1)
                .setTimeToLastByte(2)
                .setDistributedTraceIntrinsics(dtIntrinsics)
                .setSyntheticsIds(new SyntheticsIds("a", "b", "c"))
                .setSyntheticsInfo(new SyntheticsInfo("someVal", "myType", syntheticsAttr))
                .setTimeoutCause(TimeoutCause.SEGMENT)
                .setPriority(0.03f)
                .build();

        Map<String, Object> agentAttr = new HashMap<>();
        agentAttr.put("aAttr", "aVal");

        transactionEvent.setAgentAttributes(agentAttr);

        transactionEvents.add(transactionEvent);

        dataSender.sendAnalyticsEvents(111, 1, transactionEvents);
        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
    }


    private void sendCustomEvent() throws Exception {
        Collection<CustomInsightsEvent> customEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");

        CustomInsightsEvent customInsightsEvent = new CustomInsightsEvent("Custom", 14231424L, userAttributes, 23434);

        customEvents.add(customInsightsEvent);

        dataSender.sendCustomAnalyticsEvents(24, 1, customEvents);
        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
    }


    private void sendLogEvent() throws Exception {
        Collection<LogEvent> logEvents = new ArrayList<>();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("attr", "val");
        LogEvent logEvent = new LogEvent(attrs, 0.332f);
        logEvents.add(logEvent);
        dataSender.sendLogEvents(logEvents);
        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
    }

    @Test
    public void testMetrics() throws Exception {
        dataSender.sendMetricData(2L, 3L, Collections.emptyList());
        MetricData metricData1 = MetricData.create(
                MetricName.create("Other/myMetric", "myScope"),
                null,
                new StatsImpl(5, 2, 0, 3, 2));
        MetricData metricData2 = MetricData.create(
                MetricName.create("Other/myMetric2", "myScope2"),
                1233,
                new StatsImpl(5, 2, 0, 3, 2));

        dataSender.sendMetricData(1L, 4L, Arrays.asList(metricData1, metricData2));

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.eq("[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"metric_data\":[null,1,4,[[{\"name\":\"Other/myMetric\",\"scope\":\"myScope\"},[5,2.0,2.0,0.0,3.0,2.0]],[1233,[5,2.0,2.0,0.0,3.0,2.0]]]]}]")
        );
    }

    @Test
    public void testTransactionTraces() throws Exception {
        TransactionTrace trace = createTransactionTrace();
        dataSender.sendTransactionTraceData(Collections.singletonList(trace));

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.eq("[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"transaction_sample_data\":[null,[[1234124512345234,5000,\"Frontend/dude\",\"/dude\",\"eAFljkELwjAMhf9LzqXWzV12E7x4EEF30x3qFrTYdZqmioz9d+NAL0LCg/eR93KYZ/lini2Kjxayahg/czCqMMYo2G23FUxOraDpOx3wSehdo+0ZA2smG+KtJ9YR6YHkMUa9smz3GFqk/c9cdzdfYWRQwCIb5EvfQq0GSHK5ZCZ3SkKgHOCKrwxKeFifMINRgQuCQ3TNhLln6yvXIZS5NoKnV/4i8m9ELp2E9yS1OpET+zhrU4swjvUbG8BXsg==\",\"guid\",null,false,null,null]]]}]")
        );
    }

    static class TestRootTracer extends MethodExitTracer {

        public TestRootTracer() {
            super(new ClassMethodSignature("Test", "dude", "()V"), Transaction.getTransaction());
        }

        @Override
        protected void doFinish(int opcode, Object returnValue) {
        }
    }

    private TransactionData createTransactionData() {
        MethodExitTracer rootTracer = new TestRootTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        Mockito.when(tracer.getEndTime()).thenReturn(5000000000L);
        // duration of the tracer is no longer used in the calculation
        Mockito.when(tracer.getDuration()).thenReturn(3000000000L);
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key2", "value2");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key3", "value3");
        long startTime = 1234124512345234L;
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";

        return new TransactionDataTestBuilder(APP_NAME, ServiceFactory.getConfigService().getAgentConfig(APP_NAME), tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(requestParams)
                .setUserParams(userParams)
                .setAgentParams(agentParams)
                .setErrorParams(null)
                .setIntrinsics(null)
                .build();
    }

    private TransactionTrace createTransactionTrace() {
        return TransactionTrace.getTransactionTrace(createTransactionData());
    }


    @Test
    public void testSqlTraces() throws Exception {
        Transaction transaction = Transaction.getTransaction();

        SqlTraceConfig sqlTraceConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getSqlTraceConfig();

        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setUri("http://jvm.agent.uri");
        transaction.setDispatcher(dispatcher);

        TransactionData data = new TransactionData(transaction, 100);
        Tracer tracer = new DefaultTracer(transaction, new ClassMethodSignature("ClassName", "methodName",
                "methodDesc"), null, null, TracerFlags.DISPATCHER);
        SlowQueryInfo slowQueryInfo = DataSenderServerlessSqlUtil.SlowQueryInfoWrapper(data, tracer, "select * from person", "select ? from ?", sqlTraceConfig);

        SqlTrace sqlTrace = slowQueryInfo.asSqlTrace();
        dataSender.sendSqlTraceData(Collections.singletonList(sqlTrace));

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.argThat(actual -> SQL_CONSOLE_PAYLOAD_PATTERN.matcher(actual).matches()));
    }

    @Test
    public void testNoOps() throws Exception {
        Assert.assertEquals(0, dataSender.getAgentCommands().size());
        dataSender.sendModules(null);
        dataSender.sendCommandResults(null);
        dataSender.shutdown(1000);
        Assert.assertEquals(0, dataSender.sendProfileData(null).size());
        Assert.assertEquals(0, dataSender.connect(null).size());

        dataSender.commitAndFlush();
        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(FILE_PAYLOAD_MATCHER),
                Mockito.eq("[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{}]")
        );
    }

    private void setupServiceManager(Map<String, Object> settings) {
        MockServiceManager serviceManager = new MockServiceManager();
        settings.put("app_name", APP_NAME);
        Map<String, Object> serverlessModeSettings  = new HashMap<>();
        serverlessModeSettings.put("enabled", true);
        settings.put("serverless_mode", serverlessModeSettings);
        serviceManager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
        serviceManager.setTransactionTraceService(Mockito.mock(TransactionTraceService.class));
        serviceManager.setTransactionService(Mockito.mock(TransactionService.class));
        serviceManager.setTransactionEventsService(Mockito.mock(TransactionEventsService.class));
        serviceManager.setHarvestService(Mockito.mock(HarvestService.class));
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.setDistributedTraceService(new DistributedTraceServiceImpl());

        serviceManager.setAttributesService(new AttributesService());
        serviceManager.setRPMServiceManager(new MockRPMServiceManager());
    }

}
