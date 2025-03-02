/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.kafka.clients.producer;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.ClientType;
import com.nr.instrumentation.kafka.NewRelicMetricsReporter;
import org.apache.kafka.clients.Metadata;
import org.apache.kafka.common.metrics.Metrics;

import java.util.logging.Level;

@Weave(originalName = "org.apache.kafka.clients.producer.KafkaProducer")
public class KafkaProducer_Instrumentation<K, V> {

    private final Metrics metrics = Weaver.callOriginal();
    private final String clientId = Weaver.callOriginal();
    private final Metadata metadata = Weaver.callOriginal();

    // It's possible for constructors to be invoked multiple times (e.g. `C() { C("some default") }` ).
    // When this happens we don't want to register the metrics reporter multiple times.
    @NewField
    private boolean metricsReporterInstalled;

    @WeaveAllConstructors
    public KafkaProducer_Instrumentation() {
        if (!metricsReporterInstalled) {
            NewRelic.getAgent().getLogger().log(Level.INFO,
                    "newrelic-kafka-clients-enhancements engaged for producer {0}", clientId);
            metrics.addReporter(new NewRelicMetricsReporter(ClientType.PRODUCER, metadata.fetch().nodes()));
            metricsReporterInstalled = true;
        }
    }
}
