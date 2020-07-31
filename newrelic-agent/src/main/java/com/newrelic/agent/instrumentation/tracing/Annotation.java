/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Annotation extends AnnotationVisitor {
    private Map<String, Object> values;
    private final TraceDetailsBuilder traceDetailsBuilder;

    public Annotation(AnnotationVisitor annotationVisitor, String desc, TraceDetailsBuilder traceDetailsBuilder) {
        super(WeaveUtils.ASM_API_LEVEL, annotationVisitor);
        this.traceDetailsBuilder = traceDetailsBuilder;
    }

    public Map<String, Object> getValues() {
        return values == null ? Collections.<String, Object> emptyMap() : values;
    }

    @Override
    public void visit(String name, Object value) {
        getOrCreateValues().put(name, value);
        super.visit(name, value);
    }

    private Map<String, Object> getOrCreateValues() {
        if (values == null) {
            values = new HashMap<>();
        }
        return values;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        List<Object> list = (List<Object>) getOrCreateValues().get(name);
        if (list == null) {
            list = new ArrayList<>();
            getOrCreateValues().put(name, list);
        }

        final List<Object> theList = list;

        AnnotationVisitor av = super.visitArray(name);
        av = new AnnotationVisitor(WeaveUtils.ASM_API_LEVEL, av) {

            @Override
            public void visit(String name, Object value) {
                super.visit(name, value);
                theList.add(value);
            }

        };

        return av;
    }

    private boolean getBoolean(String name) {
        Boolean value = (Boolean) getValues().get(name);
        return value != null && value;
    }

    public TraceDetails getTraceDetails(boolean custom) {
        String metricName = (String) getValues().get("metricName");
        boolean dispatcher = getBoolean("dispatcher");

        if (dispatcher && metricName != null) {
            traceDetailsBuilder.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, false, MetricNames.CUSTOM,
                    metricName);
        }

        List<Object> rollupMetricNames = (List<Object>) getValues().get("rollupMetricName");
        if (rollupMetricNames != null) {
            for (Object v : rollupMetricNames) {
                traceDetailsBuilder.addRollupMetricName(v.toString());
            }
        }

        return new DelegatingTraceDetails(
                traceDetailsBuilder.setMetricName(metricName).setDispatcher(dispatcher).setTracerFactoryName(
                        (String) getValues().get("tracerFactoryName")).setExcludeFromTransactionTrace(
                        getBoolean("skipTransactionTrace")).setNameTransaction(getBoolean("nameTransaction")).setCustom(
                        custom).setExcludeFromTransactionTrace(getBoolean("excludeFromTransactionTrace")).setLeaf(
                        getBoolean("leaf")).setAsync(getBoolean("async")).build()) {

            @Override
            public String getFullMetricName(String className, String methodName) {
                return metricName();
            }

        };

    }

}
