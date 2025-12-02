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

// Todo: reformat all data into a serverless spec
// Todo: Unit tests once the data is properly formatted

public class DataSenderServerless implements DataSender {

    private static final String ARN = "TMP_ARN"; // com.amazonaws:aws-lambda-java-events needs to be instrumented to grab the ARN

    private final ServerlessWriter serverlessWriter;
    private final IAgentLogger logger;
    private final DataSenderServerlessConfig config;
    private final String awsExecutionEnv;

    public DataSenderServerless(DataSenderServerlessConfig config, IAgentLogger logger, ServerlessWriter serverlessWriter) {
        this.config = config;
        this.logger = logger;
        this.serverlessWriter = serverlessWriter;
        this.awsExecutionEnv = System.getenv("AWS_EXECUTION_ENV");
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
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateTracedErrors(errors);
        writeData(telemetryData);
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> errorEvents) throws Exception {
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateErrorEvents(errorEvents);
        telemetryData.updateErrorReservoirSize(reservoirSize);
        telemetryData.updateErrorEventsSeen(eventsSeen);
        writeData(telemetryData);
    }

    @Override
    public <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<T> events) throws Exception {
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateAnalyticEvents(events);
        telemetryData.updateAnalyticReservoirSize(reservoirSize);
        telemetryData.updateAnalyticEventsSeen(eventsSeen);
        writeData(telemetryData);
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception {
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateAnalyticEvents(events);
        telemetryData.updateAnalyticReservoirSize(reservoirSize);
        telemetryData.updateAnalyticEventsSeen(eventsSeen);
        writeData(telemetryData);
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) throws Exception {
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateAnalyticEvents(events);
        telemetryData.updateAnalyticReservoirSize(events.size());
        telemetryData.updateAnalyticEventsSeen(events.size());
        writeData(telemetryData);
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, Collection<SpanEvent> events) throws Exception {
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateSpanEvents(events);
        telemetryData.updateSpanReservoirSize(reservoirSize);
        telemetryData.updateSpanEventsSeen(eventsSeen);
        writeData(telemetryData);
    }

    @Override
    public void sendMetricData(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData) throws Exception {
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateMetricData(metricData);
        telemetryData.updateMetricBeginTimeMillis(beginTimeMillis);
        telemetryData.updateMetricEndTimeMillis(endTimeMillis);
        writeData(telemetryData);
    }

    @Override
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        // The serverless data sender is not involved with profile data
        return Collections.emptyList();
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateSqlTraces(sqlTraces);
        writeData(telemetryData);
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.updateTransactionTraces(traces);
        writeData(telemetryData);
    }

    @Override
    public void sendModules(List<? extends JSONStreamAware> jarDataToSend) throws Exception {
        // Serverless is not involved in jar collection
    }

    @Override
    public void shutdown(long timeMillis) throws Exception {
        // Serverless mode does not write data on shutdown
    }

    void writeData(TelemetryData telemetryData) {
        Map<String, Object> data = telemetryData.format();
        serverlessWriter.write(createFilePayload(data), createConsolePayload(data));
    }

    String createFilePayload(Map<String, Object> data) {
        final Map<String, Object> metadata = getMetadata();
        final List<Object> payload = Arrays.asList(2, "NR_LAMBDA_MONITORING", metadata, compressAndEncode(JSONObject.toJSONString(data)));
        return JSONArray.toJSONString(payload);
    }

    String createConsolePayload(Map<String, Object> data) {
        final Map<String, Object> metadata = getMetadata();
        final List<Object> payload = Arrays.asList(2, "NR_LAMBDA_MONITORING", metadata, data);
        return JSONArray.toJSONString(payload);
    }

    private Map<String, Object> getMetadata() {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("protocol_version", 16);
        metadata.put("arn", ARN);
        metadata.put("execution_environment", awsExecutionEnv);
        metadata.put("agent_version", config.getAgentVersion());
        metadata.put("metadata_version", 2);
        metadata.put("agent_language", "java");
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
