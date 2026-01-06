/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.config.ServerlessConfig;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.model.ApdexPerfZone;
import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.CountedDuration;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.model.SyntheticsIds;
import com.newrelic.agent.model.SyntheticsInfo;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventBuilder;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsImpl;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTrace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DataSenderServerlessImplTest {
    private ServerlessWriter serverlessWriter;

    private DataSenderServerlessImpl dataSender = null;

    @Mock
    public IAgentLogger logger;

    @Mock
    public ServerlessConfig serverlessConfig;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        serverlessWriter = Mockito.mock(ServerlessWriter.class);

        Mockito.when(serverlessConfig.getArn()).thenReturn("TMP_ARN");
        Mockito.when(serverlessConfig.getFunctionVersion()).thenReturn("15");

        DataSenderServerlessConfig config = new DataSenderServerlessConfig("9.0.0", serverlessConfig);
        this.dataSender = new DataSenderServerlessImpl(config, logger, serverlessWriter);
    }

    @Test
    public void testTracedErrors() throws Exception {
        TracedError tracedError = Mockito.mock(TracedError.class);
        Mockito.when(tracedError.getTimestampInMillis()).thenReturn(10L);
        Mockito.when(tracedError.getMessage()).thenReturn("message");
        Mockito.when(tracedError.getPath()).thenReturn("/path");
        Mockito.when(tracedError.getExceptionClass()).thenReturn("exceptionClass");
        HashMap<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put(AttributeNames.REQUEST_URI, "localhost:8080");
        Mockito.when(tracedError.getAgentAtts()).thenReturn(agentAttributes);
        Map intrinsicAttrs = new HashMap<>();
        intrinsicAttrs.put("attr1", "val1");
        Mockito.when(tracedError.getIntrinsicAtts()).thenReturn(intrinsicAttrs);
        Mockito.when(tracedError.stackTrace()).thenReturn(Arrays.asList("stackTrace1", "stackTrace2"));
        Mockito.when(tracedError.getTransactionGuid()).thenReturn("transactionGuid");

        dataSender.sendErrorData(Collections.singletonList(tracedError));

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(filePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAAHWPQQrCMBBF7/LXQVtXpTtx4QXclVLGdLDBmNbMRITSu5sigi7czePPf8zM4BjH2PWkhLoJyXvTNGVhsJ1IBxjcWIQunCd+Wp7UjeHgSQRmRhKOe9XozklZUM+LgQuZgzi7MiinJWo8yJfIaTYF/akg8j2x6CZFlxf9aMkPo2hdFVWxVkTJXjuNZDlf+MbTSiXMF+3Qmo+r++fKliBk1yeOyfVo23Z5AeHRwnwDAQAA\"]";
                    return filePayload.equals(expected);
                }),

                Mockito.argThat(consolePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"error_data\":[null,[[10,\"/path\",\"message\",\"exceptionClass\",{\"userAttributes\":{},\"intrinsics\":{\"attr1\":\"val1\"},\"agentAttributes\":{\"request.uri\":\"localhost:8080\"},\"stack_trace\":[\"stackTrace1\",\"stackTrace2\"],\"request_uri\":\"localhost:8080\"},\"transactionGuid\"]]]}]";
                    return consolePayload.equals(expected);
                })
        );
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
                Mockito.argThat(filePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAAC3NMQoDIRQE0LtMbWNrlyI3CNuIyCf+QhANX11IxLtHl23fDDMDLFLE88m5+UCNYGzuKamBy6qvzBlGKwhXlrNE8TX+eJHWU1k7EB6tCQzCQQkK7ftZKV5CudK7xZKf+wNzbfa72nd1A91AFzjn5h/2EzpSkwAAAA==\"]";
                    return filePayload.equals(expected);
                }),

                Mockito.argThat(consolePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"error_event_data\":[null,{\"events_seen\":1,\"reservoir_size\":111},[[{\"dAttr\":\"dVal\",\"type\":\"TransactionError\"},{\"uAttr\":\"uVal\"},{\"aAttr\":\"aVal\"}]]]}]";
                    return consolePayload.equals(expected);
                })
        );
    }

    @Test
    public void testSpanEvents() throws Exception {
        Collection<SpanEvent> spanEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");
        SpanEvent spanEvent = SpanEvent.builder()
                .appName("myAppName")
                .priority(1.0f)
                .putAgentAttribute("aAttr", "aVal")
                .spanKind("producer")
                .putIntrinsic("iAttr", "iVal")
                .putAllUserAttributes(userAttributes)
                .timestamp(11232344)
                .build();

        spanEvents.add(spanEvent);
        dataSender.sendSpanEvents(111, 1, spanEvents);

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(filePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAADXMQQpDIQwE0LtkLQW37nqGQjciEmoWUsmXqB9a8e4N8rucN8NMaBU50kncY8KO4DyPUsyEbS02IgZnDQg1kvPIElv+kpK1y3g/98PtnTmBgypHGi8SMNA/VVfw0FZTvvcuGvMTCyy9HxeMP+AFuCGEsH5J/geKnQAAAA==\"]";
                    return filePayload.equals(expected);
                }),

                Mockito.argThat(consolePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"span_event_data\":[null,{\"events_seen\":1,\"reservoir_size\":111},[[{\"span.kind\":\"producer\",\"type\":\"Span\",\"iAttr\":\"iVal\"},{\"uAttr\":\"uVal\"},{\"aAttr\":\"aVal\"}]]]}]";
                    return consolePayload.equals(expected);
                })
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
                Mockito.argThat(filePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAAG2PwYqDMBCG32XOoTRqe/BWtr2VPS29iMhsHUpAoyQTqRXffSdqWRY25JD5+OafyQRosRnZ3CsayHJVIyPkhQ1NoyZYmK88kYU8VeDIkxs64ypvXgS5To+zKooJ6uCQTSeW3svZ7RXUJ2YHOdQ3bEBKCf5GT+dfM1oWW8kBftrP+FLAYx/Bl0Pr8b6YAk1LnrHtpSvT2SFJs0OaxKsTncyyadimhTgtAtwALqBUsuQW/RE8dy381/XHu3aPS/z/GreKw9sry/kHuhZqjzwBAAA=\"]";
                    return filePayload.equals(expected);
                }),

                Mockito.argThat(consolePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"analytic_event_data\":[null,{\"events_seen\":3,\"reservoir_size\":136},[[{\"duration\":10000.0,\"dAttr\":\"dVal\",\"databaseDuration\":1.0,\"name\":\"txnName\",\"type\":\"Transaction\",\"timestamp\":1414523453253251212},{\"uAttr\":\"uVal\"},{\"aAttr\":\"aVal\"}],[{\"type\":\"Custom\"},{\"uAttr\":\"uVal\"},{}],[{\"type\":\"LogEvent\"},{\"attr\":\"val\"},{}]]]}]";
                    return consolePayload.equals(expected);
                })
        );

        // Verify the buffer actually cleared
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAAKuuBQBDv6ajAgAAAA==\"]",
                "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{}]"
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

        CustomInsightsEvent customInsightsEvent = Mockito.mock(CustomInsightsEvent.class);
        Mockito.when(customInsightsEvent.getUserAttributesCopy()).thenReturn(userAttributes);
        Mockito.when(customInsightsEvent.getType()).thenReturn("Custom");
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
        MetricData metricData = MetricData.create(
                MetricName.create("Other/myMetric", "myScope"),
                101,
                new StatsImpl(5, 2, 0, 3, 2));

        dataSender.sendMetricData(1L, 2L, Collections.singletonList(metricData));

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(filePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAAKtWyk0tKcpMjk9JLElUsorOK83J0THUMdKJjq5WKk7OL0hVslLKrQwGs3SU8hJzQQL+JRmpRfq5lb5gvUq1OtGmOkZ6BmBsDMQGEHYsENQCALg9059iAAAA\"]";
                    return filePayload.equals(expected);
                }),

                Mockito.argThat(consolePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"metric_data\":[null,1,2,[[{\"scope\":\"myScope\",\"name\":\"Other/myMetric\"},[5,2.0,2.0,3.0,0.0,2.0]]]]}]";
                    return consolePayload.equals(expected);
                })
        );
    }

    @Test
    public void testTransactionTraces() throws Exception {
        TransactionTrace trace = createTransactionTrace();
        dataSender.sendTransactionTraceData(Collections.singletonList(trace));

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(filePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAAD1PTYuDQAz9L+88bMftpXgrnvawK5TSi4gEjVWITpmJZRfpf++MwkIIycv7ICvU0xyo1dHNTaDpIdx0pIS8mhcRU1VZlpnMWmtQ6sD+oL8zDMS1JIMLmp/sycJstPWValekhktZXmFW9GdVjxz9jQSRUX1Gv2PybIdRuogujE1bGwQ38ceGF0IhxLDpr0jrN+vgOtT/nHT+oSlKcWfdXeoYR3vaM4VF8n0ZO5jtnZ4k8D7Cc3CLb/krWdavNxSd8FALAQAA\"]";
                    return filePayload.equals(expected);
                }),

                Mockito.argThat(consolePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"transaction_sample_data\":[null,[[111,1000,\"Other/txn\",\"localhost:8080\",[111,{},{},[111,1111,\"ROOT\",{\"fAttr\":\"fVal\"},[[200,300,\"childValue\",{},[],\"some.childClass\",\"myChildMethod\"]],\"some.className\",\"getValue\"],{\"attr\":\"val\"}],\"guid\",null,false,null,\"resourceId\"]]]}]";
                    return consolePayload.equals(expected);
                })
        );
    }

    private TransactionTrace createTransactionTrace() {
        TransactionTrace trace = Mockito.mock(TransactionTrace.class);
        Mockito.when(trace.getStartTime()).thenReturn(111L);
        Mockito.when(trace.getDuration()).thenReturn(1000L);
        Mockito.when(trace.getRootMetricName()).thenReturn("Other/txn");
        Mockito.when(trace.getRequestUri()).thenReturn("localhost:8080");

        TransactionSegment segment = Mockito.mock(TransactionSegment.class);
        Mockito.when(segment.getStartTime()).thenReturn(111L);
        Mockito.when(segment.getEndTime()).thenReturn(1111L);
        Mockito.when(segment.getMetricName()).thenReturn("ROOT");

        Map filteredAttrs = new HashMap<>();
        filteredAttrs.put("fAttr", "fVal");
        Mockito.when(segment.getFilteredAttributes()).thenReturn(filteredAttrs);

        TransactionSegment childSegment = Mockito.mock(TransactionSegment.class);
        Mockito.when(childSegment.getStartTime()).thenReturn(200L);
        Mockito.when(childSegment.getEndTime()).thenReturn(300L);
        Mockito.when(childSegment.getMetricName()).thenReturn("childValue");
        Mockito.when(childSegment.getChildren()).thenReturn(Collections.emptyList());
        Mockito.when(childSegment.getClassName()).thenReturn("some.childClass");
        Mockito.when(childSegment.getMethodName()).thenReturn("myChildMethod");
        Mockito.when(childSegment.getFilteredAttributes()).thenReturn(Collections.EMPTY_MAP);

        Mockito.when(segment.getChildren()).thenReturn(Collections.singletonList(childSegment));
        Mockito.when(segment.getClassName()).thenReturn("some.className");
        Mockito.when(segment.getMethodName()).thenReturn("getValue");

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("attr", "val");
        Mockito.when(trace.getTraceDetailsAsList()).thenReturn(Arrays.asList(111L,
                Collections.EMPTY_MAP, Collections.EMPTY_MAP, segment, attrs));
        Mockito.when(trace.getGuid()).thenReturn("guid");
        Mockito.when(trace.getSyntheticsResourceId()).thenReturn("resourceId");
        return trace;
    }


    @Test
    public void testSqlTraces() throws Exception {
        SqlTrace sqlTrace = Mockito.mock(SqlTrace.class);
        Mockito.when(sqlTrace.getBlameMetricName()).thenReturn("blameMetricName");
        Mockito.when(sqlTrace.getUri()).thenReturn("uri");
        Mockito.when(sqlTrace.getId()).thenReturn(111L);
        Mockito.when(sqlTrace.getQuery()).thenReturn("query");
        Mockito.when(sqlTrace.getMetricName()).thenReturn("metricName");
        Mockito.when(sqlTrace.getCallCount()).thenReturn(10);
        Mockito.when(sqlTrace.getTotal()).thenReturn(3L);
        Mockito.when(sqlTrace.getMin()).thenReturn(1L);
        Mockito.when(sqlTrace.getMax()).thenReturn(5L);
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        Mockito.when(sqlTrace.getParameters()).thenReturn(params);
        dataSender.sendSqlTraceData(Collections.singletonList(sqlTrace));

        Mockito.verify(serverlessWriter, Mockito.times(0)).write(Mockito.any(), Mockito.any());
        dataSender.commitAndFlush();

        Mockito.verify(serverlessWriter, Mockito.times(1)).write(
                Mockito.argThat(filePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAAKtWKi7MiS8pSkxOjU9JLElUsoqOjlZKyknMTfVNLSnKTPYDspR0lEqLMpV0DA0NdZQKS1OLKoEiuUjShgY6xjqGOqY6Sh4mxZ6OMOBdEl6ZbVYS5uMV7J1d5usdahhWmB7qWGZaHhlQbgFSYasUGxtbCwCJyRAjhQAAAA==\"]";
                    return filePayload.equals(expected);
                }),

                Mockito.argThat(consolePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{\"sql_trace_data\":[[[\"blameMetricName\",\"uri\",111,\"query\",\"metricName\",10,3,1,5,\"H4sIAAAAAAAAAKtWyk6tVLJSKkvMKU1VqgUAv5wYPw8AAAA=\"]]]}]";
                    return consolePayload.equals(expected);
                })
        );
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
                Mockito.argThat(filePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},\"H4sIAAAAAAAAAKuuBQBDv6ajAgAAAA==\"]";
                    return filePayload.equals(expected);
                }),

                Mockito.argThat(consolePayload -> {
                    String expected = "[2,\"NR_LAMBDA_MONITORING\",{\"agent_version\":\"9.0.0\",\"protocol_version\":16,\"agent_language\":\"java\",\"execution_environment\":null,\"arn\":\"TMP_ARN\",\"metadata_version\":2,\"function_version\":\"15\"},{}]";
                    return consolePayload.equals(expected);
                })
        );
    }

}
