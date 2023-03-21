/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;

public class MetricsConstants {
    public static final boolean KAFKA_METRICS_DEBUG = NewRelic.getAgent().getConfig().getValue("kafka.metrics.debug.enabled", false);

    public static final boolean METRICS_AS_EVENTS = NewRelic.getAgent().getConfig().getValue("kafka.metrics.as_events.enabled", false);

    public static final long REPORTING_INTERVAL_IN_SECONDS = NewRelic.getAgent().getConfig().getValue("kafka.metrics.interval", 30);

    public static final String METRIC_PREFIX = "MessageBroker/Kafka/Internal/";

    public static final String METRICS_EVENT_TYPE = "KafkaMetrics";

    public static final String NODE_PREFIX = "MessageBroker/Kafka/Nodes/";
}
