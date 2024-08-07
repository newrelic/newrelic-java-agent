/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

public class Metrics {

    // Serialization/Deserialization metrics
    public static final String DESERIALIZATION_TIME_METRIC_BASE = "MessageBroker/Kafka/Deserialization/";
    public static final String SERIALIZATION_TIME_METRIC_BASE = "MessageBroker/Kafka/Serialization/";

    // Rebalance metrics
    public static final String REBALANCE_REVOKED_BASE = "MessageBroker/Kafka/Rebalance/Revoked/";
    public static final String REBALANCE_ASSIGNED_BASE = "MessageBroker/Kafka/Rebalance/Assigned/";

}
