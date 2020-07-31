/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.custom;

import java.util.List;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.TraceClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.tracing.DelegatingTraceDetails;
import com.newrelic.agent.instrumentation.tracing.ParameterAttributeName;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.util.Strings;

/**
 * Stores the information for a extension "point cut". This data will be then be turned into a TracerDetails object.
 */
public class ExtensionClassAndMethodMatcher implements TraceClassAndMethodMatcher {

    private final ClassMatcher classMatcher;
    private final MethodMatcher methodMatcher;
    private final TraceDetails traceDetails;

    /**
     * Called by the newer xml stuff for server or local xml instrumentation.
     *
     * @param reportedParams
     */
    public ExtensionClassAndMethodMatcher(Extension extension, Pointcut pointcut, String metricPrefix,
            ClassMatcher classMatcher, MethodMatcher methodMatcher, boolean custom,
            List<ParameterAttributeName> reportedParams, InstrumentationType instType) {
        String metricName = pointcut.getMetricNameFormat();

        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;

        // If the `transactionType` is set to `"web"` then mark this as a web transaction.
        boolean webTransaction = false;
        if ("web".equals(pointcut.getTransactionType())) {
            webTransaction = true;
        }

        traceDetails = TraceDetailsBuilder.newBuilder()
                .setMetricName(metricName)
                .setMetricPrefix(getMetricPrefix(metricName, metricPrefix))
                .setNameTransaction(pointcut.getNameTransaction() != null)
                .setIgnoreTransaction(pointcut.isIgnoreTransaction())
                .setExcludeFromTransactionTrace(pointcut.isExcludeFromTransactionTrace())
                .setLeaf(pointcut.isLeaf())
                .setDispatcher(pointcut.isTransactionStartPoint())
                .setCustom(custom)
                .setWebTransaction(webTransaction)
                .setInstrumentationSourceName(pointcut.getClass().getName())
                .setInstrumentationType(instType)
                .setInstrumentationSourceName(extension.getName())
                .setParameterAttributeNames(reportedParams)
                .build();
    }

    private String getMetricPrefix(String metricName, String metricPrefix) {
        if (metricName != null) {
            return null;
        }
        return metricPrefix;
    }

    /**
     * This is called by the old crufty yaml code and old crufty tests.
     */
    public ExtensionClassAndMethodMatcher(String extensionName, String metricName, String metricPrefix,
            ClassMatcher classMatcher, MethodMatcher methodMatcher, boolean dispatcher, boolean skipTransTrace,
            boolean leaf, boolean ignoreTrans, String tracerFactoryName) {

        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;

        traceDetails = TraceDetailsBuilder.newBuilder()
                .setMetricName(metricName)
                .setMetricPrefix(getMetricPrefix(metricName, metricPrefix))
                .setDispatcher(dispatcher)
                .setExcludeFromTransactionTrace(skipTransTrace)
                .setLeaf(leaf)
                .setIgnoreTransaction(ignoreTrans)
                .setInstrumentationSourceName(extensionName)
                .setInstrumentationType(InstrumentationType.CustomYaml)
                .setTracerFactoryName(tracerFactoryName)
                .setCustom(true)
                .build();
    }

    @Override
    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    @Override
    public TraceDetails getTraceDetails() {
        final String metricName = traceDetails.metricName();
        final String metricPrefix = traceDetails.metricPrefix();
        return new DelegatingTraceDetails(traceDetails) {
            @Override
            public String getFullMetricName(String pClassName, String pMethodName) {
                if (metricPrefix == null && metricName == null) {
                    return null;
                } else if (metricPrefix == null) {
                    // metric name is not null
                    return getStringWhenMetricPrefixNull();
                } else if (metricName == null) {
                    // metric prefix is not null
                    return Strings.join('/', metricPrefix, "${className}", pMethodName);
                } else {
                    // both are not null - should never get here
                    return Strings.join('/', metricPrefix, metricName);
                }
            }

            private String getStringWhenMetricPrefixNull() {
                if (dispatcher()) {
                    return metricName;
                } else if (metricName.startsWith(MetricNames.OTHER_TRANSACTION)) {
                    return metricName;
                } else if (metricName.startsWith(MetricNames.SEGMENT_DELIMITER_STRING)) {
                    return MetricNames.OTHER_TRANSACTION + metricName;
                } else {
                    return Strings.join('/', MetricNames.OTHER_TRANSACTION, metricName);
                }
            }
        };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classMatcher == null) ? 0 : classMatcher.hashCode());
        result = prime * result + ((methodMatcher == null) ? 0 : methodMatcher.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExtensionClassAndMethodMatcher other = (ExtensionClassAndMethodMatcher) obj;
        if (classMatcher == null) {
            if (other.classMatcher != null) {
                return false;
            }
        } else if (!classMatcher.equals(other.classMatcher)) {
            return false;
        }
        if (methodMatcher == null) {
            if (other.methodMatcher != null) {
                return false;
            }
        } else if (!methodMatcher.equals(other.methodMatcher)) {
            return false;
        }
        return true;
    }

}
