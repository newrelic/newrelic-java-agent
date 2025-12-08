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

public class DataSenderServerlessImpl implements DataSender {

    private static final String ARN = "TMP_ARN"; // com.amazonaws:aws-lambda-java-events needs to be instrumented to grab the ARN

    private final ServerlessWriter serverlessWriter;
    private final IAgentLogger logger;
    private final DataSenderServerlessConfig config;
    private final String awsExecutionEnv;

    public DataSenderServerlessImpl(DataSenderServerlessConfig config, IAgentLogger logger, ServerlessWriter serverlessWriter) {
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
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateTracedErrors(errors);
        writeData(telemetryBuffer);
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> errorEvents) throws Exception {
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateErrorEvents(errorEvents);
        telemetryBuffer.updateErrorReservoirSize(reservoirSize);
        telemetryBuffer.updateErrorEventsSeen(eventsSeen);
        writeData(telemetryBuffer);
    }

    @Override
    public <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<T> events) throws Exception {
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateAnalyticEvents(events);
        telemetryBuffer.updateAnalyticReservoirSize(reservoirSize);
        telemetryBuffer.updateAnalyticEventsSeen(eventsSeen);
        writeData(telemetryBuffer);
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception {
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateAnalyticEvents(events);
        telemetryBuffer.updateCustomEventsReservoirSize(reservoirSize);
        telemetryBuffer.updateCustomEventsSeen(eventsSeen);
        writeData(telemetryBuffer);
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) throws Exception {
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateAnalyticEvents(events);
        telemetryBuffer.updateLogEventsReservoir(events.size());
        telemetryBuffer.updateLogEventsSeen(events.size());
        writeData(telemetryBuffer);
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, Collection<SpanEvent> events) throws Exception {
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateSpanEvents(events);
        telemetryBuffer.updateSpanReservoirSize(reservoirSize);
        telemetryBuffer.updateSpanEventsSeen(eventsSeen);
        writeData(telemetryBuffer);
    }

    @Override
    public void sendMetricData(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData) throws Exception {
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateMetricData(metricData);
        telemetryBuffer.updateMetricBeginTimeMillis(beginTimeMillis);
        telemetryBuffer.updateMetricEndTimeMillis(endTimeMillis);
        writeData(telemetryBuffer);
    }

    @Override
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        // The serverless data sender is not involved with profile data
        return Collections.emptyList();
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateSqlTraces(sqlTraces);
        writeData(telemetryBuffer);
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        TelemetryBuffer telemetryBuffer = new TelemetryBuffer();
        telemetryBuffer.updateTransactionTraces(traces);
        writeData(telemetryBuffer);
    }

    @Override
    public void sendModules(List<? extends JSONStreamAware> jarDataToSend) throws Exception {
        // Serverless is not involved in jar collection
    }

    @Override
    public void shutdown(long timeMillis) throws Exception {
        // Serverless mode does not write data on shutdown
    }

    void writeData(TelemetryBuffer telemetryBuffer) {
        JSONObject data = telemetryBuffer.formatJson();
        serverlessWriter.write(createFilePayload(data), createConsolePayload(data));
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
