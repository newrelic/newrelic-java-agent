/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.Deflater;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import com.newrelic.agent.transport.DataSenderWriter;

class SqlTraceImpl implements SqlTrace, JSONStreamAware {

    private final String blameMetricName;
    private final String metricName;
    private final String uri;
    private final String query;
    private final long id;
    private final int callCount;
    private final long total;
    private final long max;
    private final long min;
    private final Map<String, Object> parameters;

    public SqlTraceImpl(SlowQueryInfo info) {
        blameMetricName = info.getBlameMetricName();
        metricName = info.getMetricName();
        uri = info.getRequestUri();
        query = info.getQuery();
        id = info.getId();
        callCount = info.getCallCount();
        total = info.getTotalInMillis();
        min = info.getMinInMillis();
        max = info.getMaxInMillis();
        parameters = info.getParameters();
    }

    @Override
    public String getBlameMetricName() {
        return blameMetricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public int getCallCount() {
        return callCount;
    }

    @Override
    public long getMax() {
        return max;
    }

    @Override
    public long getMin() {
        return min;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(blameMetricName, uri, id, query, metricName, callCount, total, min,
                max, getData(out)), out);
    }

    private Object getData(Writer out) {
        return DataSenderWriter.getJsonifiedOptionallyCompressedEncodedString(parameters, out, Deflater.BEST_SPEED);
    }
}