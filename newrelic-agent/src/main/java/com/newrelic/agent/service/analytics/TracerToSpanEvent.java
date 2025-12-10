/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.json.AttributeFilters;
import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.SpanError;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.tracing.W3CTraceState;
import com.newrelic.agent.tracing.W3CTraceStateSupport;
import com.newrelic.agent.util.TimeConversion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.newrelic.agent.MetricNames.QUEUE_TIME;
import static com.newrelic.agent.attributes.AttributeNames.QUEUE_DURATION;
import static com.newrelic.agent.config.ConfigConstant.MAX_USER_ATTRIBUTES;

/**
 * Utility used to conditionally turn a Tracer into a SpanEvent
 */
public class TracerToSpanEvent {
    private static final Set<String> UNWANTED_SPAN_ATTRIBUTES = new HashSet<>(Arrays.asList(
            "databaseCallCount", "databaseDuration", "externalCallCount", "externalDuration", "gcCumulative",
            "memcacheDuration", "nr.apdexPerfZone", "totalTime", "transactionSubType", "transactionType",
            "nr.alternatePathHashes", "nr.guid", "nr.pathHash", "nr.referringTransactionGuid",
            "nr.tripId", "host.displayName", "process.instanceName", "nr.syntheticsJobId", "nr.syntheticsMonitorId",
            "nr.syntheticsResourceId",
            // Distributed Tracing intrinsics that we want on the transaction but not spans.
            "parentSpanId", "priority", "sampled", "guid", "traceId"
    ));
    private final Map<String, SpanErrorBuilder> errorBuilderForApp;
    private final AttributeFilter filter;
    private final Supplier<Long> timestampSupplier;
    private final EnvironmentService environmentService;
    private final TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics;
    private final SpanErrorBuilder defaultSpanErrorBuilder;

    public TracerToSpanEvent(Map<String, SpanErrorBuilder> errorBuilderForApp, EnvironmentService environmentService,
            TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics, SpanErrorBuilder defaultSpanErrorBuilder) {
        this(
                errorBuilderForApp,
                AttributeFilters.SPAN_EVENTS_ATTRIBUTE_FILTER,
                SpanEventFactory.DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER,
                environmentService,
                transactionDataToDistributedTraceIntrinsics,
                defaultSpanErrorBuilder);
    }

    TracerToSpanEvent(Map<String, SpanErrorBuilder> errorBuilderForApp, AttributeFilter filter, Supplier<Long> timestampSupplier,
            EnvironmentService environmentService,
            TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics, SpanErrorBuilder defaultSpanErrorBuilder) {
        this.errorBuilderForApp = errorBuilderForApp;
        this.filter = filter;
        this.timestampSupplier = timestampSupplier;
        this.environmentService = environmentService;
        this.transactionDataToDistributedTraceIntrinsics = transactionDataToDistributedTraceIntrinsics;
        this.defaultSpanErrorBuilder = defaultSpanErrorBuilder;
    }

    public SpanEvent createSpanEvent(Tracer tracer, TransactionData transactionData, TransactionStats transactionStats, boolean isRoot,
            boolean removeNonEssentialAttrs) {
        SpanProxy spanProxy = transactionData.getSpanProxy();

        SpanEventFactory builder = new SpanEventFactory(transactionData.getApplicationName(), filter, timestampSupplier, removeNonEssentialAttrs)
                .setGuid(tracer.getGuid())
                .setClmAttributes(tracer.getAgentAttributes())
                .setTraceId(spanProxy.getOrCreateTraceId())
                .setSampled(transactionData.sampled())
                .setParentId(getParentId(tracer, transactionData))
                .setTransactionId(transactionData.getGuid())
                .setDurationInSeconds((float) tracer.getDuration() / TimeConversion.NANOSECONDS_PER_SECOND)
                .setName(tracer.getTransactionSegmentName())
                .setTimestamp(tracer.getStartTimeInMillis())
                .setPriority(transactionData.getPriority())
                .setExternalParameterAttributes(tracer.getExternalParameters())
                .setAgentAttributesMarkedForSpans(tracer.getAgentAttributeNamesForSpans(), tracer.getAgentAttributes())
                .setStackTraceAttributes(tracer.getAgentAttributes())
                .setIsRootSpanEvent(isRoot);

        builder = maybeSetError(tracer, transactionData, isRoot, builder);
        builder = maybeSetGraphQLAttributes(tracer, builder);

        W3CTraceState traceState = spanProxy.getInitiatingW3CTraceState();
        if (traceState != null) {
            if (isRoot && traceState.getGuid() != null) {
                builder.setTrustedParent(traceState.getGuid());
            }
            Set<String> vendorKeys = W3CTraceStateSupport.buildVendorKeys(traceState);
            builder.setTracingVendors(vendorKeys);
        }

        LimitedSizeHashMap<String, Object> spanUserAttributes = new LimitedSizeHashMap<>(removeNonEssentialAttrs ? 0 : MAX_USER_ATTRIBUTES);

        // order matters here because we don't want transaction attributes to overwrite tracer attributes. This would be the case if there were 64
        // transaction attributes and they got added first to the span attributes map. Then none of the tracer attributes would make it in due
        // to the limit of 64 attributes.
        spanUserAttributes.putAll(tracer.getCustomAttributes());

        if (isRoot) {
            copyTransactionAttributesToRootSpanBuilder(builder, transactionData, spanUserAttributes, transactionStats);
        }

        builder.putAllUserAttributes(spanUserAttributes);
        return builder.build();
    }

    private SpanEventFactory maybeSetGraphQLAttributes(Tracer tracer, SpanEventFactory builder) {
        Map<String, Object> agentAttributes = tracer.getAgentAttributes();
        boolean containsGraphQLAttributes = agentAttributes.keySet().stream().anyMatch(key -> key.contains("graphql"));
        if (containsGraphQLAttributes){
            agentAttributes.entrySet().stream()
                    .filter(e -> e.getKey().contains("graphql"))
                    .forEach(e -> builder.putAgentAttribute(e.getKey(), e.getValue()));
        }
        return builder;
    }

    private SpanEventFactory maybeSetError(Tracer tracer, TransactionData transactionData, boolean isRoot, SpanEventFactory builder) {
        SpanErrorBuilder spanErrorBuilder = errorBuilderForApp.get(transactionData.getApplicationName());
        spanErrorBuilder = spanErrorBuilder == null ? defaultSpanErrorBuilder : spanErrorBuilder;

        if (spanErrorBuilder.areErrorsEnabled()) {
            final SpanError spanError = spanErrorBuilder.buildSpanError(
                    tracer,
                    isRoot,
                    transactionData.getResponseStatus(),
                    transactionData.getStatusMessage(),
                    transactionData.getThrowable());

            return builder.setSpanError(spanError);
        }
        return builder;
    }

    private void copyTransactionAttributesToRootSpanBuilder(SpanEventFactory builder, TransactionData transactionData,
            LimitedSizeHashMap<String, Object> spanUserAttributes, TransactionStats transactionStats) {
        builder.putIntrinsicAttribute("transaction.name", transactionData.getPriorityTransactionName().getName());
        Integer port = environmentService.getEnvironment().getAgentIdentity().getServerPort();
        builder.putAgentAttribute("port", port);
        if (transactionStats.getUnscopedStats().getStatsMap().containsKey(QUEUE_TIME)) {
            builder.putAgentAttribute(QUEUE_DURATION, transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(QUEUE_TIME).getTotal());
        }

        copyTransactionIntrinsicAttributes(builder, transactionData);
        copyDistributedTraceIntrinsicAttributes(builder, transactionData);
        copyTransactionAgentAttributes(builder, transactionData);
        copyTransactionUserAttributes(transactionData, spanUserAttributes);
    }

    private void copyTransactionIntrinsicAttributes(SpanEventFactory builder, TransactionData transactionData) {
        Map<String, Object> intrinsicAttributes = transactionData.getIntrinsicAttributes();
        Map<String, ?> filteredIntrinsics = filterAttributes(intrinsicAttributes);
        builder.putAllAgentAttributes(filteredIntrinsics);
    }

    private void copyDistributedTraceIntrinsicAttributes(SpanEventFactory builder, TransactionData transactionData) {
        Map<String, Object> distributedTraceIntrinsicAttributes = transactionDataToDistributedTraceIntrinsics
                .buildDistributedTracingIntrinsics(transactionData, false);

        if (distributedTraceIntrinsicAttributes != null) {
            Map<String, ?> filteredIntrinsics = filterAttributes(distributedTraceIntrinsicAttributes);
            builder.putAllAgentAttributes(filteredIntrinsics);
        }
    }

    private void copyTransactionAgentAttributes(SpanEventFactory builder, TransactionData transactionData) {
        Map<String, Object> agentAttributes = transactionData.getAgentAttributes();
        Map<String, ?> filteredAgentAttributes = filterAttributes(agentAttributes);
        builder.putAllAgentAttributes(filteredAgentAttributes);
        Map<String, Map<String, String>> prefixedAttributes = transactionData.getPrefixedAttributes();
        Map<String, ?> filteredPrefixedAttributes = filterAttributes(AttributesUtils.appendAttributePrefixes(prefixedAttributes));
        builder.putAllAgentAttributes(filteredPrefixedAttributes);
    }

    private void copyTransactionUserAttributes(TransactionData transactionData, LimitedSizeHashMap<String, Object> spanUserAttributes) {
        Map<String, Object> userAttributes = transactionData.getUserAttributes();
        Map<String, ?> filteredUserAttributes = filterAttributes(userAttributes);
        spanUserAttributes.putAllIfAbsent(filteredUserAttributes);
    }

    private Map<String, ?> filterAttributes(Map<String, ?> intrinsicAttributes) {
        return Maps.filterKeys(intrinsicAttributes, key -> !UNWANTED_SPAN_ATTRIBUTES.contains(key));
    }

    private String getParentId(Tracer tracer, TransactionData transactionData) {
        // This is the non cross_process_only case where we "parent" using the parent tracer
        // or the inbound payload id if this is the first/root tracer and we have an inbound payload
        Tracer parentSegment = AbstractTracer.getParentTracerWithSpan(tracer.getParentTracer());

        if (parentSegment != null) {
            return parentSegment.getGuid();
        }

        DistributedTracePayloadImpl inboundPayload = transactionData.getInboundDistributedTracePayload();
        if (inboundPayload != null) {
            // If we have an inbound payload we can use the id from the payload since it should be the id of the span that initiated this trace
            return inboundPayload.getGuid();
        }

        if (transactionData.getW3CTraceParent() != null) {
            return transactionData.getW3CTraceParent().getParentId();
        }

        return null;
    }
}
