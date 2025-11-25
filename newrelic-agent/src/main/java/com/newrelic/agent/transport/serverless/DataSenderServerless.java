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
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Todo: reformat all data into a serverless spec
// Todo: Unit tests once the data is properly formatted

public class DataSenderServerless implements DataSender {

    private final ServerlessWriter serverlessWriter;
    private final IAgentLogger logger;
    private final DataSenderServerlessConfig dataSenderServerlessConfig;

    public DataSenderServerless(DataSenderServerlessConfig dataSenderServerlessConfig, IAgentLogger logger, ServerlessWriter serverlessWriter) {
        this.dataSenderServerlessConfig = dataSenderServerlessConfig;
        this.logger = logger;
        this.serverlessWriter = serverlessWriter;
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
        JSONObject metadata = new JSONObject();
        metadata.put("errors", errors);
        serverlessWriter.write(metadata);
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> errorEvents) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("errorEvents", errorEvents);
        serverlessWriter.write(metadata);
    }

    @Override
    public <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<T> events) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("events", events);
        serverlessWriter.write(metadata);
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("events", events);
        serverlessWriter.write(metadata);
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("events", events);
        serverlessWriter.write(metadata);
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, Collection<SpanEvent> events) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("events", events);
        serverlessWriter.write(metadata);
    }

    @Override
    public void sendMetricData(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("metricData", metricData);
        serverlessWriter.write(metadata);
    }

    @Override
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("profiles", profiles);
        serverlessWriter.write(metadata);
        return Collections.emptyList();
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("sqlTraces", sqlTraces);
        serverlessWriter.write(metadata);
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("traces", traces);
        serverlessWriter.write(metadata);
    }

    @Override
    public void sendModules(List<? extends JSONStreamAware> jarDataToSend) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("jarDataToSend", jarDataToSend);
        serverlessWriter.write(metadata);
    }

    @Override
    public void shutdown(long timeMillis) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("timeMillis", timeMillis);
        serverlessWriter.write(metadata);
    }
}
