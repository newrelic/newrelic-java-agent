/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.kafka.clients.consumer;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.nr.instrumentation.kafka.NewRelicMetricsReporter;

import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.metrics.Metrics;

@Weave(originalName = "org.apache.kafka.clients.consumer.KafkaConsumer")
public abstract class KafkaConsumer_Instrumentation<K, V> {

    // It's possible for constructors to be invoked multiple times (e.g. `C() { C("some default") }` ).
    // When this happens we don't want to register the metrics reporter multiple times.
    @NewField
    private boolean metricsReporterInstalled;

    @WeaveAllConstructors
    public KafkaConsumer_Instrumentation() {

        String clientId = clientInstanceId(Duration.ofSeconds(1)).toString();
        Metrics metrics = (Metrics) metrics();

        if (!metricsReporterInstalled) {
            NewRelic.getAgent().getLogger().log(Level.INFO,
                    "newrelic-kafka-clients-enhancements engaged for consumer {0}", clientId);
            metrics.addReporter(new NewRelicMetricsReporter());
            metricsReporterInstalled = true;
        }
    }

    public abstract Uuid clientInstanceId(Duration timeout);

    public abstract Map<MetricName, ? extends Metric> metrics();
}
