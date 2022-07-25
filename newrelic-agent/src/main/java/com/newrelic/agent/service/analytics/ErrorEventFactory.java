/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeValidator;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.environment.AgentIdentity;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.errors.DeadlockTraceError;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.util.TimeConversion;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.json.AttributeFilters.ERROR_EVENTS_ATTRIBUTE_FILTER;
import static com.newrelic.agent.model.ErrorEvent.*;

public class ErrorEventFactory {

    public static ErrorEvent create(String appName, TracedError tracedError, float priority) {
        return new ErrorEvent(appName, tracedError.getTimestampInMillis(), priority, new HashMap<>(tracedError.getErrorAtts()),
                tracedError.getExceptionClass(), truncateIfNecessary(tracedError.getMessage()), isErrorExpected(tracedError),
                UNKNOWN, UNASSIGNED, UNASSIGNED, UNASSIGNED, UNASSIGNED, UNASSIGNED, UNASSIGNED, UNASSIGNED, null,
                null, null, null, null, getPortUsingServiceManagerIfPossible(), null, null, Collections.<String, Object>emptyMap(),
                null, ERROR_EVENTS_ATTRIBUTE_FILTER);
    }

    public static ErrorEvent create(String appName, TracedError tracedError, TransactionData transactionData,
                                    TransactionStats transactionStats) {
        long timestamp = tracedError.getTimestampInMillis();
        Map<String, Object> userAttributes = buildUserAttributes(appName, transactionData);
        String errorMessage = truncateIfNecessary(tracedError.getMessage());
        String transactionName = transactionData.getPriorityTransactionName().getName();
        return new ErrorEvent(appName, timestamp, transactionData.getPriority(), userAttributes,
                tracedError.getExceptionClass(), errorMessage,
                isErrorExpected(tracedError), transactionName,
                (float) transactionData.getDurationInMillis() / TimeConversion.MILLISECONDS_PER_SECOND,
                getMetricTotal(transactionStats, MetricNames.QUEUE_TIME), getMetricTotal(transactionStats, MetricNames.EXTERNAL_ALL),
                getMetricTotal(transactionStats, DatastoreMetrics.ALL), getMetricTotal(transactionStats, MetricNames.GC_CUMULATIVE),
                getMetricCount(transactionStats, DatastoreMetrics.ALL), getMetricCount(transactionStats, MetricNames.EXTERNAL_ALL),
                transactionData.getGuid(), transactionData.getReferrerGuid(), transactionData.getSyntheticsResourceId(),
                transactionData.getSyntheticsMonitorId(), transactionData.getSyntheticsJobId(), getPortUsingServiceManagerIfPossible(),
                transactionData.getTimeoutCause() == null ? null : transactionData.getTimeoutCause().cause, getTripId(transactionData),
                getDistributedTraceIntrinsics(transactionData), buildAgentAttributes(appName, transactionData), ERROR_EVENTS_ATTRIBUTE_FILTER);
    }

    private static float getMetricTotal(TransactionStats transactionStats, String metricName) {
        if (metricExists(transactionStats, metricName)) {
            return getMetric(transactionStats, metricName).getTotal();
        }
        return ErrorEvent.UNASSIGNED;
    }

    private static int getMetricCount(TransactionStats transactionStats, String metricName) {
        if (metricExists(transactionStats, metricName)) {
            return getMetric(transactionStats, metricName).getCallCount();
        }
        return 0;
    }

    private static boolean metricExists(TransactionStats transactionStats, String metricName) {
        return transactionStats.getUnscopedStats().getStatsMap().containsKey(metricName);
    }

    private static ResponseTimeStats getMetric(TransactionStats transactionStats, String metricName) {
        return transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(metricName);
    }

    private static String truncateIfNecessary(String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length > ConfigConstant.MAX_ERROR_MESSAGE_SIZE) {
            return AttributeValidator.truncateString(value, ConfigConstant.MAX_ERROR_MESSAGE_SIZE);
        }
        return value;
    }


    private static String getTripId(TransactionData transactionData) {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig().isEnabled() ? transactionData.getTripId() : null;
    }

    private static boolean isErrorExpected(TracedError tracedError) {
        return !tracedError.incrementsErrorMetric() && !(tracedError instanceof DeadlockTraceError);
    }

    private static int getPortUsingServiceManagerIfPossible() {
        // Who needs DI?
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            EnvironmentService environmentService = serviceManager.getEnvironmentService();
            if (environmentService != null) {
                Environment environment = environmentService.getEnvironment();
                if (environment != null) {
                    AgentIdentity agentIdentity = environment.getAgentIdentity();
                    if (agentIdentity != null) {
                        Integer serverPort = agentIdentity.getServerPort();
                        if (serverPort != null) {
                            return serverPort;
                        }
                    }
                }
            }
        }
        return UNASSIGNED_INT;
    }


    private static Map<String, Object> getDistributedTraceIntrinsics(TransactionData transactionData) {
        DistributedTracingConfig distributedTracingConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig();
        if (!distributedTracingConfig.isEnabled()) {
            return null;
        }
        // Better CAT
        DistributedTraceService distributedTraceService = ServiceFactory.getDistributedTraceService();
        return distributedTraceService.getIntrinsics(
                transactionData.getInboundDistributedTracePayload(), transactionData.getGuid(),
                transactionData.getTraceId(), transactionData.getTransportType(),
                transactionData.getTransportDurationInMillis(),
                transactionData.getLargestTransportDurationInMillis(),
                transactionData.getParentId(), transactionData.getParentSpanId(),
                transactionData.getPriority());
    }

    private static Map<String, Object> buildAgentAttributes(String appName, TransactionData transactionData) {
        if (!ServiceFactory.getAttributesService().isAttributesEnabledForErrorEvents(appName)) {
            return null;
        }
        Map<String, Object> agentAttrs = new HashMap<>(transactionData.getAgentAttributes());
        // request/message parameters are sent up in the same bucket as agent attributes
        agentAttrs.putAll(AttributesUtils.appendAttributePrefixes(transactionData.getPrefixedAttributes()));

        if (transactionData.getThrowable() != null) {
            String spanId = transactionData.getThrowable().spanId;
            if (spanId != null) {
                agentAttrs.put("spanId", spanId);
            }
        }
        return agentAttrs;
    }

    private static Map<String, Object> buildUserAttributes(String appName, TransactionData transactionData) {
        Map<String, Object> userAttributes = new HashMap<>();
        // trans events take user and agent atts - any desired intrinsics should have already been grabbed
        if (ServiceFactory.getAttributesService().isAttributesEnabledForErrorEvents(appName)) {
            userAttributes.putAll(transactionData.getUserAttributes());
            userAttributes.putAll(transactionData.getErrorAttributes());
        }
        return userAttributes;
    }
}
