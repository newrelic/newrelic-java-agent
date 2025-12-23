/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.util.StackTraces;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class TransactionSegment implements JSONStreamAware {

    /**
     * This property should be used when the transaction segment has reduced its stack trace because the parent has the
     * rest of the stack trace. If the full stack trace is included in this segment, then the property name backtrace
     * should still be used.
     */
    private static final String PARTIAL_TRACE = "partialtrace";
    private static final Pattern INSERT_INTO_VALUES_STATEMENT = Pattern.compile(
            "\\s*insert\\s+into\\s+([^\\s(,]*)\\s+values.*", Pattern.CASE_INSENSITIVE);
    private static final String URL_PARAMETER_NAME = "http.url";
    public static final String ASYNC_EXCLUSIVE = "exclusive_duration_millis";
    private static final double NANO_TO_MILLI = 1000000.0;
    private final String appName;
    private String metricName;
    private final List<TransactionSegment> children;
    private final long entryTimestamp;
    private long exitTimestamp;
    private final Map<String, Object> tracerAttributes;
    private int callCount = 1;
    private final String uri;
    private final SqlObfuscator sqlObfuscator;
    private final TransactionTracerConfig ttConfig;
    private final List<StackTraceElement> parentStackTrace;

    private final ClassMethodSignature classMethodSignature;

    public TransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator, long startTime,
            Tracer tracer) {
        this(ttConfig, tracer.getTransactionActivity().getTransaction().getApplicationName(), sqlObfuscator, startTime, tracer, null);
    }

    TransactionSegment(TransactionTracerConfig ttConfig, String appName, SqlObfuscator sqlObfuscator, long startTime, Tracer tracer,
            TransactionSegment childSegment) {
        this.appName = appName;
        this.ttConfig = ttConfig;
        this.sqlObfuscator = sqlObfuscator;
        this.metricName = getMetricName(tracer);
        this.uri = getUri(tracer);
        if (childSegment == null) {
            children = new ArrayList<>();
        } else {
            children = new ArrayList<>(1);
            children.add(childSegment);
        }
        entryTimestamp = tracer.getStartTimeInMilliseconds() - startTime;
        exitTimestamp = tracer.getEndTimeInMilliseconds() - startTime;
        tracerAttributes = getTracerAttributes(tracer);
        classMethodSignature = tracer.getClassMethodSignature();

        parentStackTrace = getParentStackTrace(tracer);
    }

    private List<StackTraceElement> getParentStackTrace(Tracer tracer) {
        if (tracer.getParentTracer() != null) {
            return (List<StackTraceElement>) tracer.getParentTracer().getAgentAttribute(
                    DefaultTracer.BACKTRACE_PARAMETER_NAME);
        }
        return null;
    }

    private Map<String, Object> getTracerAttributes(Tracer tracer) {
        if (tracer instanceof SqlTracerExplainInfo) {
            Object sql = ((SqlTracerExplainInfo) tracer).getSql();
            String sqlHashValue = ((SqlTracer) tracer).getNormalizedSqlHashValue();
            if (sql != null) {
                tracer.setAgentAttribute(SqlTracer.SQL_PARAMETER_NAME, sql);
            }
            if (sqlHashValue != null) {
                tracer.setAgentAttribute(SqlTracer.SQL_HASH_VALUE, sqlHashValue);
            }
        }
        // this has to be a double or else the UI will get rid of the number unless it is greater than 1000.
        // do not use TimeUnit.convert - the rounding is way off for this one
        double exclusiveDur = tracer.getExclusiveDuration() / NANO_TO_MILLI;
        tracer.setAgentAttribute(ASYNC_EXCLUSIVE, exclusiveDur);
        return tracer.getAgentAttributes();
    }

    /*
     * This should only be called on the ROOT node. The ROOT node needs to be set to the transaction duration instead of
     * the response time.
     */
    void resetExitTimeStampInMs(long duration) {
        exitTimestamp = duration;
    }

    public static String getMetricName(Tracer tracer) {
        String metricName = tracer.getTransactionSegmentName();
        if (metricName == null || metricName.trim().length() == 0) {
            if (Agent.isDebugEnabled()) {
                throw new RuntimeException(MessageFormat.format(
                        "Encountered a transaction segment with an invalid metric name. {0}",
                        tracer.getClass().getName()));
            } else {
                metricName = tracer.getClass().getName() + "*";
            }
        }
        return metricName;
    }

    public Map<String, Object> getTraceParameters() {
        return Collections.unmodifiableMap(tracerAttributes);
    }

    private static String getUri(Tracer tracer) {
        boolean excludeRequestUri = ServiceFactory.getConfigService().getDefaultAgentConfig().getExternalTracerConfig().excludeRequestUri();
        if (excludeRequestUri) {
            return null;
        }
        return tracer.getTransactionSegmentUri();
    }

    void setMetricName(String name) {
        if (name != null && name.trim().length() > 0)
            metricName = name;
    }

    public Collection<TransactionSegment> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    /*
     * public double getDuration() { return TimeConversion.convertNanosToSeconds(response.getDuration()); }
     */

    public String getMetricName() {
        return metricName;
    }

    public void addChild(TransactionSegment sample) {
        try {
            children.add(sample);
        } catch (UnsupportedOperationException e) {
            String msg = MessageFormat.format("Unable to add transaction segment {0} to parent segment {1}", sample,
                    this);
            Agent.LOG.info(msg);
        }
    }

    @Override
    public String toString() {
        return metricName;
    }

    // getter here for testing
    public long getStartTime() {
        return entryTimestamp;
    }

    // getter here for testing
    public long getEndTime() {
        return exitTimestamp;
    }

    // getter here for testing
    public String getClassName() {
        return classMethodSignature.getClassName();
    }

    // getter here for testing
    public String getMethodName() {
        return classMethodSignature.getMethodName();
    }

    // getter here for testing
    public int getCallCount() {
        return callCount;
    }

    // getter here for testing
    public String getUri() {
        return uri;
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException {
        final Map<String, Object> params = new HashMap<>(tracerAttributes);
        processStackTraces(params);
        processSqlParams(params);

        if (callCount > 1) {
            params.put("call_count", callCount);
        }

        if ((uri != null) && (uri.length() > 0)) {
            params.put(URL_PARAMETER_NAME, uri);
        }

        final Map<String, ?> filteredAtts = ServiceFactory.getAttributesService().filterTransactionSegmentAttributes(appName, params);

        JSONArray.writeJSONString(Arrays.asList(entryTimestamp, exitTimestamp, metricName, filteredAtts, children,
                classMethodSignature.getClassName(), classMethodSignature.getMethodName()), writer);

    }

    @SuppressWarnings("unchecked")
    private void processSqlParams(Map<String, Object> params) {

        Object sqlObj = params.remove(SqlTracer.SQL_PARAMETER_NAME);
        if (sqlObj == null) {
            return;
        }

        String sql;
        Object obfuscatedSqlObject = params.remove(SqlTracer.SQL_OBFUSCATED_PARAMETER_NAME);
        if (obfuscatedSqlObject == null) {
            // This could happen if this came from a DefaultSqlTracer (built-in SQL recording)
            sql = sqlObfuscator.obfuscateSql(sqlObj.toString());
        } else if (sqlObfuscator.isObfuscating()) {
            // If we came from a custom slow query and obfuscation is on, use the value from the tracer
            sql = obfuscatedSqlObject.toString();
        } else {
            // If we came from a custom slow query and obfuscation is off, use the raw query
            sql = sqlObj.toString();
        }

        if (sql == null) {
            return;
        }

        if (INSERT_INTO_VALUES_STATEMENT.matcher(sql).matches()) {
            int maxLength = ttConfig.getInsertSqlMaxLength();
            sql = truncateSql(sql, maxLength);
        }
        if (ttConfig.isLogSql()) {
            Agent.LOG.log(Level.INFO, MessageFormat.format("{0} SQL: {1}", ttConfig.getRecordSql(), sql));
            return;
        }
        params.put(sqlObfuscator.isObfuscating() ? SqlTracer.SQL_OBFUSCATED_PARAMETER_NAME
                : SqlTracer.SQL_PARAMETER_NAME, sql);
    }

    private void processStackTraces(Map<String, Object> params) {
        // add stack trace if present
        List<StackTraceElement> backtrace = (List<StackTraceElement>) params.remove(DefaultTracer.BACKTRACE_PARAMETER_NAME);
        if (backtrace != null) {
            List<StackTraceElement> preStackTraces = StackTraces.scrubAndTruncate(backtrace);
            List<String> postParentRemovalTrace = StackTraces.toStringListRemoveParent(preStackTraces, parentStackTrace);

            if (preStackTraces.size() == postParentRemovalTrace.size()) {
                // keep as backtrace parameter - is full trace
                params.put(DefaultTracer.BACKTRACE_PARAMETER_NAME, postParentRemovalTrace);
            } else {
                // add as partial trace parameter
                params.put(PARTIAL_TRACE, postParentRemovalTrace);
            }

        }
    }

    public void merge(Tracer tracer) {
        callCount++;
        exitTimestamp += tracer.getDurationInMilliseconds();
    }

    public static String truncateSql(String sql, int maxLength) {
        int len = sql.length();
        if (len > maxLength) {
            return MessageFormat.format("{0}..({1} more chars)", sql.substring(0, maxLength), len - maxLength);
        } else {
            return sql;
        }
    }

}
