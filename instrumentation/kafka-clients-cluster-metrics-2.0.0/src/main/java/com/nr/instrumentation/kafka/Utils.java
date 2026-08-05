/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

public class Utils {
    public static final String KAFKA_CLUSTER_METRIC_PREFIX = "MessageBroker/Kafka/Cluster/";
    public static final String KAFKA_CLUSTER_TOPIC_SEGMENT = "/Topic/";
    public static final String KAFKA_CLUSTER_PRODUCE_SUFFIX = "/Produce";
    public static final String KAFKA_CLUSTER_CONSUME_SUFFIX = "/Consume";
}
