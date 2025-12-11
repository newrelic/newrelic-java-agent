/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.transport.DataSender;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DataSenderServerlessImpl implements DataSender {

    private static final String ARN = "TMP_ARN"; // com.amazonaws:aws-lambda-java-events needs to be instrumented to grab the ARN
    private static final String FUNCTION_VERSION = "15";

    private final ServerlessWriter serverlessWriter;
    private final IAgentLogger logger;
    private final DataSenderServerlessConfig config;
    private final String awsExecutionEnv;
    private final TelemetryBuffer buffer;

    public DataSenderServerlessImpl(DataSenderServerlessConfig config, IAgentLogger logger, ServerlessWriter serverlessWriter) {
        this.config = config;
        this.logger = logger;
        this.serverlessWriter = serverlessWriter;
        this.awsExecutionEnv = System.getenv("AWS_EXECUTION_ENV");
        this.buffer = new TelemetryBuffer();
    }

    @Override
    public Map<String, Object> connect(Map<String, Object> startupOptions) throws Exception {
        // The serverless data sender is not making any external connections
        return Collections.emptyMap();
    }

    @Override
    public List<List<?>> getAgentCommands() throws Exception {
        // The serverless data sender is not involved in agent commands
        return Collections.emptyList();
    }

    @Override
    public void sendCommandResults(Map<Long, Object> commandResults) throws Exception {
        // The serverless data sender is not involved in agent commands
    }

    @Override
    public void sendErrorData(List<TracedError> errors) throws Exception {
        buffer.updateTracedErrors(errors);
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> errorEvents) throws Exception {
        buffer.updateErrorEvents(errorEvents);
        buffer.updateErrorReservoirSize(reservoirSize);
        buffer.updateErrorEventsSeen(eventsSeen);
    }

    @Override
    public <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<T> events) throws Exception {
        buffer.updateAnalyticEvents(events);
        buffer.updateAnalyticReservoirSize(reservoirSize);
        buffer.updateAnalyticEventsSeen(eventsSeen);
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception {
        buffer.updateAnalyticEvents(events);
        buffer.updateCustomEventsReservoirSize(reservoirSize);
        buffer.updateCustomEventsSeen(eventsSeen);
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) throws Exception {
        buffer.updateAnalyticEvents(events);
        buffer.updateLogEventsReservoir(events.size());
        buffer.updateLogEventsSeen(events.size());
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, Collection<SpanEvent> events) throws Exception {
        buffer.updateSpanEvents(events);
        buffer.updateSpanReservoirSize(reservoirSize);
        buffer.updateSpanEventsSeen(eventsSeen);
    }

    @Override
    public void sendMetricData(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData) throws Exception {
        buffer.updateMetricData(metricData);
        buffer.updateMetricBeginTimeMillis(beginTimeMillis);
        buffer.updateMetricEndTimeMillis(endTimeMillis);
    }

    @Override
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        // The serverless data sender is not involved with profile data
        return Collections.emptyList();
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        buffer.updateSqlTraces(sqlTraces);
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        buffer.updateTransactionTraces(traces);
    }

    @Override
    public void sendModules(List<? extends JSONStreamAware> jarDataToSend) throws Exception {
        // Serverless is not involved in jar collection
    }

    @Override
    public void shutdown(long timeMillis) throws Exception {
        // Serverless mode does not write data on shutdown
    }

    @Override
    public void commitAndFlush() throws Exception {
        writeData();
    }

    void writeData() {
        JSONObject data = this.buffer.formatJson();
        serverlessWriter.write(createFilePayload(data), createConsolePayload(data));
        this.buffer.clear();
    }

    String createFilePayload(JSONObject data) {
        final List<Object> payload = Arrays.asList(2, "NR_LAMBDA_MONITORING", getMetadata(),
                compressAndEncode(JSONObject.toJSONString(data).replace("\\/","/")));

        return JSONArray.toJSONString(payload).replace("\\/","/");
    }

    String createConsolePayload(JSONObject data) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(2);
        jsonArray.add("NR_LAMBDA_MONITORING");
        jsonArray.add(getMetadata());
        jsonArray.add(data);
        return jsonArray.toJSONString().replace("\\/","/");
    }

    private Map<String, Object> getMetadata() {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("protocol_version", 16);
        metadata.put("arn", ARN);
        metadata.put("execution_environment", awsExecutionEnv);
        metadata.put("agent_version", config.getAgentVersion());
        metadata.put("metadata_version", 2);
        metadata.put("agent_language", "java");
        metadata.put("function_version", FUNCTION_VERSION);
        return metadata;
    }

    /**
     * gzip compress and base64 encode.
     *
     * @param source String to be compressed and encoded
     * @return String compressed and encoded
     */
    public static String compressAndEncode(String source) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(output);
            gzip.write(source.getBytes(UTF_8));
            gzip.flush();
            gzip.close();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ignored) {
        }
        return "";
    }

}
