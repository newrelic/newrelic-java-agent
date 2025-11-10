/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import org.apache.kafka.common.metrics.KafkaMetric;

public class MetricNameUtil {
    static final String METRIC_PREFIX = "MessageBroker/Kafka/Internal/";

    static String buildDisplayName(final KafkaMetric metric) {
        return String.format("%s/%s %s", metric.metricName().group(), metric.metricName().name(), metric.metricName().tags());
    }

    static String buildMetricName(final KafkaMetric metric) {
        return buildMetricName(metric, null);
    }

    static String buildMetricName(final KafkaMetric metric, final String nameOverride) {
        final String name = nameOverride != null ? nameOverride : metric.metricName().name();
        final String metricPrefix = METRIC_PREFIX + metric.metricName().group() + "/";

        final String clientId = metric.metricName().tags().get("client-id");
        if (clientId == null) {
            return metricPrefix + name;
        }

        // is it a per-topic metric?
        final String topic = metric.metricName().tags().get("topic");
        if (topic != null) {
            return metricPrefix + "topic/" + topic + "/client/" + clientId + "/" + name;
        }

        // is it a per-node metric?
        String nodeId = metric.metricName().tags().get("node-id");
        if (nodeId != null) {
            nodeId = normalizeNodeId(nodeId);
            return metricPrefix + "node/" + nodeId + "/client/" + clientId + "/" + name;
        }

        return metricPrefix + "client/" + clientId + "/" + name;
    }

    private static String normalizeNodeId(final String nodeId) {
        //
        // sometimes node IDs get weird. let's try to clean things up a bit.
        //

        final String[] parts = nodeId.split("-", 2);
        if (parts.length != 2) {
            //
            // a strange node ID that doesn't conform to the expected pattern. leave it be.
            //
            return nodeId;
        }

        final int num;
        try {
            num = Integer.parseInt(parts[1]);
        } catch (final NumberFormatException e) {
            //
            // non-numeric value in the node ID. weird, but OK.
            //
            return nodeId;
        }

        //
        // negative node IDs are used for seed brokers (i.e. initial metadata bootstrap)
        // the negative values are pretty useless in practice and just act as placeholders
        // for the metadata request. once the metadata request is complete we know the real
        // broker IDs and things get more interesting.
        //
        // return "seed" for negative node IDs since it's probably more useful to users
        // than some confusing pseudo-ID.
        //
        if (num < 0) {
            return "seed";
        }

        //
        // try to detect coordinator node IDs. what is this nonsense? I'm so glad you asked.
        //
        // group coordinator node IDs get munged in order to separate the coordinator
        // "control plane" from the data plane. this is achieved by subtracting the
        // true node ID from Integer.MAX_VALUE. here we just unmunge the node ID to
        // get the true ID of the group coordinator to report something more useful
        // to users.
        //
        // note there's no "guaranteed" way to avoid conflicts across the node ID
        // "namespace" so we can't actually tell the difference between a coordinator
        // node ID and a "regular" node ID, but here we assume that node IDs aren't
        // typically huge (in practice I believe they're limited to fairly small but
        // configurable values on the broker side anyway)
        //
        final int coordinatorNodeId = Integer.MAX_VALUE - num;
        if (coordinatorNodeId > 0 && coordinatorNodeId < (Integer.MAX_VALUE & 0xff000000)){
            return "coordinator-" + coordinatorNodeId;
        }

        //
        // fall back to the unmodified node ID that was passed in (this should be the typical case)
        //
        return nodeId;
    }

    private MetricNameUtil() {
        // prevents instantiation
    }
}
