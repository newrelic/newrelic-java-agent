/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.config.DatastoreConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.SlowQueryDatastoreParameters;
import com.newrelic.api.agent.SlowQueryWithInputDatastoreParameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintain a list of slow queries via tracers. This class is thread safe.
 */
public class DefaultSlowQueryListener implements SlowQueryListener {

    private static final int MAX_SQL_TRACERS = 175;

    private final double thresholdInMillis;
    private volatile BoundedConcurrentCache<String, SlowQueryInfo> slowQueryInfoCache;
    private final SqlObfuscator sqlObfuscator;

    public DefaultSlowQueryListener(String appName, double thresholdInMillis) {
        this.thresholdInMillis = thresholdInMillis;
        
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getSqlObfuscator(appName);
        this.sqlObfuscator = SqlObfuscator.getCachingSqlObfuscator(sqlObfuscator);
    }

    @Override
    public <T> void noticeTracer(Tracer tracer, SlowQueryDatastoreParameters<T> slowQueryDatastoreParameters) {
        if (tracer.getDurationInMilliseconds() > -1) {
            T rawQuery = slowQueryDatastoreParameters.getRawQuery();
            QueryConverter<T> queryConverter = slowQueryDatastoreParameters.getQueryConverter();
            if (rawQuery == null || queryConverter == null) {
                // Ignore tracer
                return;
            }

            String rawQueryString = queryConverter.toRawQueryString(rawQuery);
            System.out.println("DUF- noticeTracer Raw " + rawQueryString);
            if (rawQueryString == null || rawQueryString.trim().isEmpty()) {
                // Ignore tracer
                return;
            }

            String obfuscatedQueryString = queryConverter.toObfuscatedQueryString(rawQuery);
            System.out.println("DUF- noticeTracer Ob " + rawQueryString);
            if (obfuscatedQueryString == null) {
                // Ignore tracer if no obfuscated query is provided
                return;
            }

            // Handle an "input query" from an ORM or a framework that automatically generates queries
            if (slowQueryDatastoreParameters instanceof SlowQueryWithInputDatastoreParameters) {
                handleInputQuery(tracer, (SlowQueryWithInputDatastoreParameters) slowQueryDatastoreParameters);
            }

            // This allows transaction traces to show slow queries directly in the trace details
            tracer.setAgentAttribute(SqlTracer.SQL_PARAMETER_NAME, rawQueryString);
            tracer.setAgentAttribute(SqlTracer.SQL_OBFUSCATED_PARAMETER_NAME, obfuscatedQueryString);

            DatastoreConfig datastoreConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDatastoreConfig();
            boolean allUnknown = slowQueryDatastoreParameters.getHost() == null
                    && slowQueryDatastoreParameters.getPort() == null
                    && slowQueryDatastoreParameters.getPathOrId() == null;
            if (datastoreConfig.isInstanceReportingEnabled() && !allUnknown) {
                tracer.setAgentAttribute(DatastoreMetrics.DATASTORE_HOST, DatastoreMetrics.replaceLocalhost(slowQueryDatastoreParameters.getHost()));
                tracer.setAgentAttribute(DatastoreMetrics.DATASTORE_PORT_PATH_OR_ID, DatastoreMetrics.getIdentifierOrPort(
                        slowQueryDatastoreParameters.getPort(), slowQueryDatastoreParameters.getPathOrId()));
            }

            if (datastoreConfig.isDatabaseNameReportingEnabled() && slowQueryDatastoreParameters.getDatabaseName() != null) {
                tracer.setAgentAttribute(DatastoreMetrics.DB_INSTANCE, slowQueryDatastoreParameters.getDatabaseName());
            }

            if (slowQueryInfoCache == null) {
                slowQueryInfoCache = new BoundedConcurrentCache<>(MAX_SQL_TRACERS);
            }

            SlowQueryInfo existingInfo = slowQueryInfoCache.get(obfuscatedQueryString);
            if (existingInfo != null) {
                // Aggregate tracers by SQL.
                existingInfo.aggregate(tracer);
                slowQueryInfoCache.putReplace(obfuscatedQueryString, existingInfo);
            } else {
                SlowQueryInfo sqlInfo = new SlowQueryInfo(null, tracer, rawQueryString, obfuscatedQueryString,
                        tracer.getTransactionActivity().getTransaction().getAgentConfig().getSqlTraceConfig());
                sqlInfo.aggregate(tracer);
                slowQueryInfoCache.putIfAbsent(obfuscatedQueryString, sqlInfo);
            }
        }
    }

    @Override
    public List<SlowQueryInfo> getSlowQueries() {
        if (slowQueryInfoCache == null) {
            return Collections.emptyList();
        }
        return slowQueryInfoCache.asList();
    }

    private <T, I> void handleInputQuery(Tracer tracer,
            SlowQueryWithInputDatastoreParameters<T, I> slowQueryWithInputDatastoreParameters) {
        String inputQueryLabel = slowQueryWithInputDatastoreParameters.getInputQueryLabel();
        I rawInputQuery = slowQueryWithInputDatastoreParameters.getRawInputQuery();
        QueryConverter<I> inputQueryConverter = slowQueryWithInputDatastoreParameters.getRawInputQueryConverter();

        if (inputQueryLabel == null || rawInputQuery == null || inputQueryConverter == null) {
            return; // Ignore if any input values are null
        }

        String rawInputQueryString = inputQueryConverter.toRawQueryString(rawInputQuery);
        if (rawInputQueryString == null || rawInputQueryString.trim().isEmpty()) {
            return; // Ignore if the raw input query is null
        }

        String obfuscatedInputQueryString = null;
        if (sqlObfuscator.isObfuscating()) {
            // If obfuscation is on, lets run the query through the obfuscator
            obfuscatedInputQueryString = inputQueryConverter.toObfuscatedQueryString(rawInputQuery);
            if (obfuscatedInputQueryString == null) {
                return; // Ignore if we get back a null obfuscated input query
            }
        }

        if (obfuscatedInputQueryString != null && obfuscatedInputQueryString.equals(rawInputQueryString)) {
            return; // Ignore if we got back an obfuscated query and it's the same as the raw query
        }

        // Everything checks out, add the required parameters and set the attribute on the tracer
        Map<String, String> inputQueryParameterValue = new HashMap<>();
        inputQueryParameterValue.put(DatastoreMetrics.INPUT_QUERY_LABEL_PARAMETER, inputQueryLabel);
        if (sqlObfuscator.isObfuscating()) {
            inputQueryParameterValue.put(DatastoreMetrics.INPUT_QUERY_QUERY_PARAMETER, obfuscatedInputQueryString);
        } else {
            inputQueryParameterValue.put(DatastoreMetrics.INPUT_QUERY_QUERY_PARAMETER, rawInputQueryString);
        }
        tracer.setAgentAttribute(DatastoreMetrics.INPUT_QUERY_ATTRIBUTE, inputQueryParameterValue);
    }

}
