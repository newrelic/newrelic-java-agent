/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.ExplainPlanExecutor;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transport.DataSenderWriter;
import com.newrelic.agent.util.TimeConversion;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.zip.Deflater;

public class TransactionTrace implements Comparable<TransactionTrace>, JSONStreamAware {

    private static final String HAS_ASYNC_CHILD_ATT = "async_wait";

    private final TransactionSegment rootSegment;
    private final List<TransactionSegment> sqlSegments;
    private final Map<ConnectionFactory, List<ExplainPlanExecutor>> sqlTracers;
    private final long duration;
    private final long startTime;
    private String requestUri;
    private final String rootMetricName;
    private final Map<String, Object> userAttributes;
    private final Map<String, Object> agentAttributes;
    private final Map<String, Object> intrinsicAttributes;
    private final long rootTracerStartTime;
    private Map<Tracer, Collection<Tracer>> children;
    private final String guid;
    private final Map<String, Map<String, String>> prefixedAttributes;
    private String syntheticsResourceId;
    private String syntheticsType;
    private String syntheticsInitiator;
    private Map<String, String> syntheticsAttributes;
    private final String applicationName;

    private TransactionTrace(TransactionData transactionData, SqlObfuscator sqlObfuscator) {
        this.applicationName = transactionData.getApplicationName();

        children = buildChildren(transactionData.getTracers());
        sqlTracers = new HashMap<>();
        Tracer tracer = transactionData.getRootTracer();
        userAttributes = new HashMap<>();
        agentAttributes = new HashMap<>();
        if (ServiceFactory.getAttributesService().isAttributesEnabledForTransactionTraces(applicationName)) {
            if (transactionData.getAgentAttributes() != null) {
                agentAttributes.putAll(transactionData.getAgentAttributes());
            }
            if (transactionData.getUserAttributes() != null) {
                userAttributes.putAll(transactionData.getUserAttributes());
            }
        }
        prefixedAttributes = transactionData.getPrefixedAttributes();
        intrinsicAttributes = getIntrinsics(transactionData);
        startTime = transactionData.getWallClockStartTimeMs();
        rootTracerStartTime = tracer.getStartTimeInMilliseconds();
        sqlSegments = new LinkedList<>();

        boolean requestUriDisabled = null == transactionData.getRequestUri(AgentConfigImpl.TRANSACTION_TRACER);
        if (requestUriDisabled) {
            this.requestUri = null;
        } else {
            this.requestUri = transactionData.getRequestUri(AgentConfigImpl.TRANSACTION_TRACER);
            if (this.requestUri == null || this.requestUri.length() == 0) {
                this.requestUri = "/Unknown";
            }
        }

        this.rootMetricName = transactionData.getBlameOrRootMetricName();
        this.guid = transactionData.getGuid();
        final String applicationName = transactionData.getApplicationName();
        rootSegment = new TransactionSegment(transactionData.getTransactionTracerConfig(), applicationName, sqlObfuscator, rootTracerStartTime,
                tracer, createTransactionSegment(transactionData.getTransactionTracerConfig(), sqlObfuscator, tracer, null));
        rootSegment.setMetricName("ROOT");
        // use the rootTraceStartTime instead of txDur from transaction time as that was giving us a rounding error
        long txDurMs = Math.max(0, transactionData.getTransactionTime().getEndTimeInMilliseconds() - rootTracerStartTime);
        // the root segment should report the transaction duration instead of the response time
        rootSegment.resetExitTimeStampInMs(txDurMs);
        duration = transactionData.getTransactionTime().getResponseTimeInMilliseconds();

        children.clear();
        children = null;
        this.syntheticsResourceId = null;
    }

    private static Map<String, Object> getIntrinsics(TransactionData transactionData) {
        Map<String, Object> intrinsicAttributes = new HashMap<>();
        if (transactionData.getIntrinsicAttributes() != null) {
            intrinsicAttributes.putAll(transactionData.getIntrinsicAttributes());
        }
        intrinsicAttributes.put("totalTime", (float) transactionData.getTransactionTime().getTotalSumTimeInNanos()
                / TimeConversion.NANOSECONDS_PER_SECOND_FLOAT);
        if (transactionData.getTransactionTime().getTimeToFirstByteInNanos() > 0) {
            intrinsicAttributes.put("timeToFirstByte",
                    (float) transactionData.getTransactionTime().getTimeToFirstByteInNanos()
                            / TimeConversion.NANOSECONDS_PER_SECOND_FLOAT);
        }
        if (transactionData.getTransactionTime().getTimetoLastByteInNanos() > 0) {
            intrinsicAttributes.put("timeToLastByte",
                    (float) transactionData.getTransactionTime().getTimetoLastByteInNanos()
                            / TimeConversion.NANOSECONDS_PER_SECOND_FLOAT);
        }
        Long gcTime = (Long) intrinsicAttributes.remove(AttributeNames.GC_TIME_PARAMETER_NAME);
        if (gcTime != null) {
            float gcTimeInSecs = (float) gcTime / TimeConversion.NANOSECONDS_PER_SECOND_FLOAT;
            intrinsicAttributes.put(AttributeNames.GC_TIME_PARAMETER_NAME, gcTimeInSecs);
        }
        Long cpuTime = (Long) intrinsicAttributes.remove(AttributeNames.CPU_TIME_PARAMETER_NAME);
        if (cpuTime != null) {
            float cpuTimeInSecs = (float) cpuTime / TimeConversion.NANOSECONDS_PER_SECOND_FLOAT;
            intrinsicAttributes.put(AttributeNames.CPU_TIME_PARAMETER_NAME, cpuTimeInSecs);
        }
        if (transactionData.isSyntheticTransaction()) {
            intrinsicAttributes.put("nr.syntheticsType", transactionData.getSyntheticsType());
            intrinsicAttributes.put("nr.syntheticsInitiator", transactionData.getSyntheticsInitiator());

            Map<String, String> attrsMap = transactionData.getSyntheticsAttributes();
            String attrName;

            for (String key : attrsMap.keySet()) {
                attrName = String.format("nr.synthetics%s", Character.toUpperCase(key.charAt(0)) + key.substring(1));
                intrinsicAttributes.put(attrName, attrsMap.get(key));
            }
        }
        return intrinsicAttributes;
    }

    @VisibleForTesting
    public static Map<Tracer, Collection<Tracer>> buildChildren(Collection<Tracer> tracers) {
        if (tracers == null || tracers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Tracer, Collection<Tracer>> children = new HashMap<>();
        for (Tracer tracer : tracers) {
            Tracer parentTracer = tracer.getParentTracer();
            while (null != parentTracer && !parentTracer.isTransactionSegment()) {
                parentTracer = parentTracer.getParentTracer();
            }
            if (tracer.getAgentAttribute("async_context") != null && parentTracer != null) {
                parentTracer.setAgentAttribute(HAS_ASYNC_CHILD_ATT, Boolean.TRUE);
            }
            Collection<Tracer> kids = children.get(parentTracer);
            if (kids == null) {
                kids = new ArrayList<>(parentTracer == null ? 1 : Math.max(1, parentTracer.getChildCount()));
                children.put(parentTracer, kids);
            }
            if (tracer.isTransactionSegment()){
                kids.add(tracer);
            }
        }
        return children;
    }

    public long getStartTime() {
        return startTime;
    }

    private static SqlObfuscator getSqlObfuscator(String appName) {
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getSqlObfuscator(appName);
        return SqlObfuscator.getCachingSqlObfuscator(sqlObfuscator);
    }

    public static TransactionTrace getTransactionTrace(TransactionData td) {
        return getTransactionTrace(td, getSqlObfuscator(td.getApplicationName()));
    }

    static TransactionTrace getTransactionTrace(TransactionData transactionData, SqlObfuscator sqlObfuscator) {
        return new TransactionTrace(transactionData, sqlObfuscator);
    }

    public TransactionSegment getRootSegment() {
        return rootSegment;
    }

    private TransactionSegment createTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
            Tracer tracer, TransactionSegment lastSibling) {
        TransactionSegment segment = tracer.getTransactionSegment(ttConfig, sqlObfuscator, rootTracerStartTime, lastSibling);
        processSqlTracer(tracer);
        Collection<Tracer> children = this.children.get(tracer);
        if (children != null) {
            TransactionSegment lastKid = null;
            for (Tracer child : children) {
                if (child.getTransactionSegmentName() != null) {
                    TransactionSegment childSegment = createTransactionSegment(ttConfig, sqlObfuscator, child, lastKid);
                    if (childSegment != lastKid) {
                        /*
                         * We used to check the childSegment's metric name for null and remove it if the name was null.
                         * After reading the code and writing a test, I do not think it is possible to have a null
                         * metric name. The transaction segment code sets it to the tracer class if it is null. I have
                         * no clue why we would ever want to send up a segment with the tracer class as the name.
                         */
                        segment.addChild(childSegment);
                        lastKid = childSegment;
                    }
                }
            }
        }
        return segment;
    }

    public Map<ConnectionFactory, List<ExplainPlanExecutor>> getExplainPlanExecutors() {
        return Collections.unmodifiableMap(sqlTracers);
    }

    private void processSqlTracer(Tracer tracer) {
        if (tracer instanceof SqlTracer) {
            SqlTracer sqlTracer = (SqlTracer) tracer;

            ExplainPlanExecutor explainExecutor = sqlTracer.getExplainPlanExecutor();
            ConnectionFactory connectionFactory = sqlTracer.getConnectionFactory();

            if (!sqlTracer.hasExplainPlan() && explainExecutor != null && connectionFactory != null) {
                List<ExplainPlanExecutor> tracers = sqlTracers.get(connectionFactory);
                if (tracers == null) {
                    tracers = new LinkedList<>();
                    sqlTracers.put(connectionFactory, tracers);
                }
                tracers.add(explainExecutor);
            }
        }
    }

    private void runExplainPlans() {
        if (!sqlTracers.isEmpty()) {
            DatabaseService dbService = ServiceFactory.getDatabaseService();
            for (Entry<ConnectionFactory, List<ExplainPlanExecutor>> entry : sqlTracers.entrySet()) {
                Agent.LOG.finer(MessageFormat.format("Running {0} explain plan(s)", entry.getValue().size()));
                Connection connection = null;
                try {
                    connection = entry.getKey().getConnection();
                    DatabaseVendor vendor = entry.getKey().getDatabaseVendor();
                    for (ExplainPlanExecutor explainExecutor : entry.getValue()) {
                        if (explainExecutor != null) {
                            explainExecutor.runExplainPlan(dbService, connection, vendor);
                        }
                    }
                } catch (Throwable t) {
                    String msg = MessageFormat.format("An error occurred executing an explain plan: {0}", t.toString());
                    if (Agent.LOG.isLoggable(Level.FINER)) {
                        Agent.LOG.log(Level.FINER, msg, t);
                    } else {
                        Agent.LOG.fine(msg);
                    }
                } finally {
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (Exception e) {
                            Agent.LOG.log(Level.FINER, "Unable to close connection", e);
                        }
                    }
                }
            }
            sqlTracers.clear();
        }
    }

    private Map<String, Object> getAgentAtts() {
        Map<String, Object> atts = new HashMap<>();
        atts.putAll(agentAttributes);
        if (prefixedAttributes != null && !prefixedAttributes.isEmpty()) {
            atts.putAll(AttributesUtils.appendAttributePrefixes(prefixedAttributes));
        }
        atts.put(AttributeNames.REQUEST_URI, requestUri);
        return atts;
    }

    private void filterAndAddIfNotEmpty(String key, Map<String, Object> wheretoAdd, Map<String, Object> toAdd) {
        Map<String, ?> output = ServiceFactory.getAttributesService().filterTransactionTraceAttributes(applicationName, toAdd);
        if (output != null && !output.isEmpty()) {
            wheretoAdd.put(key, output);
        }
    }

    private Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        if (ServiceFactory.getAttributesService().isAttributesEnabledForTransactionTraces(applicationName)) {
            filterAndAddIfNotEmpty("agentAttributes", attributes, getAgentAtts());
            // user attributes should have already been filtered for high security - this is just extra protection
            // high security is per an account - meaning it can not be different for various application names within a
            // JVM - so we can just check the default agent config
            if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                filterAndAddIfNotEmpty("userAttributes", attributes, userAttributes);
            }
        }
        // intrinsics go up even if attributes are disabled
        if (intrinsicAttributes != null && !intrinsicAttributes.isEmpty()) {
            attributes.put("intrinsics", intrinsicAttributes);
        }
        return attributes;
    }

    public Map<String, Object> getUserAttributes() {
        return new HashMap<>(userAttributes);
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException {
        runExplainPlans();

        // forcePersist and xraySessionId are obsolete attributes of the JSON.
        // We still have to send them, but with these values.
        final boolean forcePersist = false;
        final Object xraySessionId = null;

        List<Object> data = Arrays.asList(startTime, Collections.EMPTY_MAP, Collections.EMPTY_MAP, rootSegment, getAttributes());

        if (null == syntheticsResourceId) {
            JSONArray.writeJSONString(Arrays.asList(startTime, duration, rootMetricName, requestUri,
                    getData(writer, data), guid, null, forcePersist), writer);
        } else {
            JSONArray.writeJSONString(Arrays.asList(startTime, duration, rootMetricName, requestUri,
                    getData(writer, data), guid, null, forcePersist, xraySessionId, syntheticsResourceId), writer);
        }
    }

    private Object getData(Writer writer, List<Object> data) {
        return DataSenderWriter.getJsonifiedOptionallyCompressedEncodedString(data, writer, Deflater.BEST_SPEED);
    }

    protected List<TransactionSegment> getSQLSegments() {
        return sqlSegments;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} {1} ms", requestUri, duration);
    }

    @Override
    public int compareTo(TransactionTrace o) {
        return (int) (duration - o.duration);
    }

    public long getDuration() {
        return duration;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setSyntheticsResourceId(String syntheticsResourceId) {
        this.syntheticsResourceId = syntheticsResourceId;
    }

    public String getSyntheticsResourceId() {
        return this.syntheticsResourceId;
    }

    public String getRootMetricName() {
        return rootMetricName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public Map<String, Object> getIntrinsicsShallowCopy() {
        return new HashMap<>(intrinsicAttributes);
    }

}
