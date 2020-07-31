/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.util.Strings;

public class TraceDetailsBuilder {
    private String metricPrefix;
    private String metricName;
    private String tracerFactoryName;
    private boolean dispatcher;
    private boolean async;
    private boolean excludeFromTransactionTrace;
    private boolean ignoreTransaction;
    private boolean nameTransaction;
    private boolean custom;
    private boolean webTransaction;
    private TransactionName transactionName;
    // the instrumentation type and source name should match up on each index
    private List<InstrumentationType> instrumentationTypes = new ArrayList<>(3);
    private List<String> instrumentationSourceNames = new ArrayList<>(3);
    private boolean leaf;
    private final List<String> rollupMetricName = new ArrayList<>(5);
    public List<ParameterAttributeName> parameterAttributeNames;

    private TraceDetailsBuilder() {
    }

    public static TraceDetailsBuilder newBuilder() {
        return new TraceDetailsBuilder();
    }

    public static TraceDetailsBuilder newBuilder(TraceDetails traceDetails) {
        TraceDetailsBuilder builder = new TraceDetailsBuilder();

        builder.custom = traceDetails.isCustom();
        builder.dispatcher = traceDetails.dispatcher();
        builder.async = traceDetails.async();
        builder.excludeFromTransactionTrace = traceDetails.excludeFromTransactionTrace();
        builder.ignoreTransaction = traceDetails.ignoreTransaction();
        builder.instrumentationSourceNames = new ArrayList<>(traceDetails.instrumentationSourceNames());
        builder.instrumentationTypes = new ArrayList<>(traceDetails.instrumentationTypes());
        builder.metricName = traceDetails.metricName();
        builder.metricPrefix = traceDetails.metricPrefix();
        builder.transactionName = traceDetails.transactionName();
        builder.webTransaction = traceDetails.isWebTransaction();
        builder.leaf = traceDetails.isLeaf();
        builder.rollupMetricName.addAll(Arrays.asList(traceDetails.rollupMetricName()));
        builder.parameterAttributeNames = new ArrayList<>(traceDetails.getParameterAttributeNames());

        return builder;
    }

    public TraceDetails build() {
        return new DefaultTraceDetails(this);
    }

    public TraceDetailsBuilder setParameterAttributeNames(List<ParameterAttributeName> reportedParams) {
        this.parameterAttributeNames = reportedParams;
        return this;
    }

    public TraceDetailsBuilder setMetricPrefix(String metricPrefix) {
        if (metricPrefix == null) {
            this.metricPrefix = null;
        } else {
            this.metricPrefix = metricPrefix.endsWith("/") ? metricPrefix.substring(0, metricPrefix.length() - 1)
                    : metricPrefix;
        }
        return this;
    }

    public TraceDetailsBuilder setMetricName(String metricName) {
        this.metricName = metricName;
        return this;
    }

    public TraceDetailsBuilder setTracerFactoryName(String tracerFactoryName) {
        this.tracerFactoryName = tracerFactoryName;
        return this;
    }

    public TraceDetailsBuilder setDispatcher(boolean dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    public TraceDetailsBuilder setAsync(boolean async) {
        this.async = async;
        return this;
    }

    public TraceDetailsBuilder setCustom(boolean custom) {
        this.custom = custom;
        return this;
    }

    public TraceDetailsBuilder setLeaf(boolean leaf) {
        this.leaf = leaf;
        return this;
    }

    public TraceDetailsBuilder setExcludeFromTransactionTrace(boolean excludeFromTransactionTrace) {
        this.excludeFromTransactionTrace = excludeFromTransactionTrace;
        return this;
    }

    public TraceDetailsBuilder setIgnoreTransaction(boolean ignoreTransaction) {
        this.ignoreTransaction = ignoreTransaction;
        return this;
    }

    public TraceDetailsBuilder setNameTransaction(boolean nameTransaction) {
        this.nameTransaction = nameTransaction;
        return this;
    }

    public TraceDetailsBuilder setTransactionName(TransactionNamePriority namingPriority, boolean override,
            String category, String path) {
        this.transactionName = new TransactionName(namingPriority, override, category, path);
        return this;
    }

    public TraceDetailsBuilder setInstrumentationType(InstrumentationType type) {
        instrumentationTypes = new ArrayList<>(Collections.singletonList(
                type != null ? type : InstrumentationType.Unknown
        ));
        return this;
    }

    public TraceDetailsBuilder setInstrumentationSourceName(String instrumentationSourceName) {
        instrumentationSourceNames = new ArrayList<>(Collections.singletonList(
                instrumentationSourceName != null ? instrumentationSourceName : "Unknown"
        ));
        return this;
    }

    public TraceDetailsBuilder setWebTransaction(boolean webTransaction) {
        this.webTransaction = webTransaction;
        return this;
    }

    public TraceDetailsBuilder addRollupMetricName(String metricName) {
        this.rollupMetricName.add(metricName);
        return this;
    }

    public TraceDetailsBuilder merge(TraceDetails otherDetails) {
        if (metricPrefix == null) {
            metricPrefix = otherDetails.metricPrefix();
        }
        if (metricName == null) {
            metricName = otherDetails.metricName();
        }
        if (tracerFactoryName == null) {
            tracerFactoryName = otherDetails.tracerFactoryName();
        }
        if (!dispatcher) {
            dispatcher = otherDetails.dispatcher();
        }
        if (!async) {
            async = otherDetails.async();
        }
        if (!excludeFromTransactionTrace) {
            excludeFromTransactionTrace = otherDetails.excludeFromTransactionTrace();
        }
        if (!ignoreTransaction && !custom) {
            ignoreTransaction = otherDetails.ignoreTransaction();
        }
        if (transactionName == null) {
            transactionName = otherDetails.transactionName();
        }
        if (!custom) {
            custom = otherDetails.isCustom();
            if (!leaf) {
                leaf = otherDetails.isLeaf();
            }
        }
        if (!webTransaction) {
            webTransaction = otherDetails.isWebTransaction();
        }
        rollupMetricName.addAll(Arrays.asList(otherDetails.rollupMetricName()));

        instrumentationTypes.addAll(otherDetails.instrumentationTypes());
        instrumentationSourceNames.addAll(otherDetails.instrumentationSourceNames());

        parameterAttributeNames.addAll(otherDetails.getParameterAttributeNames());

        return this;
    }

    public static TraceDetails merge(TraceDetails existing, TraceDetails trace) {
        if (trace.isCustom()) {
            return TraceDetailsBuilder.newBuilder(trace).merge(existing).build();
        } else {
            return TraceDetailsBuilder.newBuilder(existing).merge(trace).build();
        }
    }

    private static final class DefaultTraceDetails implements TraceDetails {

        private final String metricPrefix;
        private final String metricName;
        private final String tracerFactoryName;
        private final TransactionName transactionName;
        private final boolean dispatcher;
        private final boolean async;
        private final boolean excludeFromTransactionTrace;
        private final boolean ignoreTransaction;
        private final boolean custom;
        private final boolean webTransaction;
        private final List<InstrumentationType> instrumentationTypes;
        private final List<String> instrumentationSourceNames;
        private final boolean leaf;
        private final String[] rollupMetricNames;
        private final List<ParameterAttributeName> parameterAttributeNames;

        protected DefaultTraceDetails(TraceDetailsBuilder builder) {
            metricName = builder.metricName;
            metricPrefix = builder.metricPrefix;
            tracerFactoryName = builder.tracerFactoryName;
            dispatcher = builder.dispatcher;
            async = builder.async;
            excludeFromTransactionTrace = builder.excludeFromTransactionTrace;
            ignoreTransaction = builder.ignoreTransaction;
            custom = builder.custom;
            if (builder.nameTransaction) {
                transactionName = custom ? TransactionName.CUSTOM_DEFAULT : TransactionName.BUILT_IN_DEFAULT;
            } else {
                transactionName = builder.transactionName;
            }
            instrumentationSourceNames = new ArrayList<>(builder.instrumentationSourceNames);
            instrumentationTypes = new ArrayList<>(builder.instrumentationTypes);
            webTransaction = builder.webTransaction;
            leaf = builder.leaf;
            rollupMetricNames = builder.rollupMetricName.toArray(new String[0]);
            parameterAttributeNames = builder.parameterAttributeNames == null ? ImmutableList.<ParameterAttributeName> of()
                    : builder.parameterAttributeNames;
        }

        @Override
        public boolean isLeaf() {
            return leaf;
        }

        @Override
        public String metricName() {
            return metricName;
        }

        @Override
        public boolean dispatcher() {
            return dispatcher;
        }

        @Override
        public String tracerFactoryName() {
            return tracerFactoryName;
        }

        @Override
        public boolean excludeFromTransactionTrace() {
            return excludeFromTransactionTrace;
        }

        @Override
        public boolean async() {
            return async;
        }

        @Override
        public String metricPrefix() {
            return metricPrefix;
        }

        @Override
        public String getFullMetricName(String className, String methodName) {
            if (metricName != null) {
                return metricName;
            } else if (metricPrefix == null) {
                return null;
            } else {
                return Strings.join('/', metricPrefix, "${className}", methodName);
            }
        }

        @Override
        public boolean ignoreTransaction() {
            return ignoreTransaction;
        }

        @Override
        public boolean isCustom() {
            return custom;
        }

        @Override
        public TransactionName transactionName() {
            return transactionName;
        }

        @Override
        public List<InstrumentationType> instrumentationTypes() {
            return instrumentationTypes;
        }

        @Override
        public List<String> instrumentationSourceNames() {
            return instrumentationSourceNames;
        }

        @Override
        public boolean isWebTransaction() {
            return webTransaction;
        }

        @Override
        public String toString() {
            return "DefaultTraceDetails [transactionName=" + transactionName + ", dispatcher=" + dispatcher
                    + ", custom=" + custom + ", instrumentationType=" + instrumentationTypes
                    + ", instrumentationSourceName=" + instrumentationSourceNames + "]";
        }

        @Override
        public String[] rollupMetricName() {
            return rollupMetricNames;
        }

        @Override
        public List<ParameterAttributeName> getParameterAttributeNames() {
            return parameterAttributeNames;
        }

    }
}
